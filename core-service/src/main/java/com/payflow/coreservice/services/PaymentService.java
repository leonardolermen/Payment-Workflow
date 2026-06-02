package com.payflow.coreservice.services;

import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.dto.payment.PaymentRequest;
import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.builder.AnalysisRequestBuilder;
import com.payflow.coreservice.client.AntiFraudClient;
import com.payflow.coreservice.dto.factory.PaymentResponseFactory;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.factory.PaymentFactory;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import com.payflow.coreservice.strategy.factory.PaymentStatusHandlerFactory;

import java.util.List;
import java.util.UUID;

import static com.payflow.commons.enums.fraud.Status_Fraud.REJECTED;

@Slf4j
@Getter
@Setter
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AntiFraudClient antiFraudClient;
    private final PaymentPersistenceHelper paymentPersistenceHelper;
    private final PaymentStatusHandlerFactory handlerFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          AntiFraudClient antiFraudClient,
                          PaymentPersistenceHelper paymentPersistenceHelper,
                          PaymentStatusHandlerFactory handlerFactory,
                          RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.antiFraudClient = antiFraudClient;
        this.paymentPersistenceHelper = paymentPersistenceHelper;
        this.handlerFactory = handlerFactory;
        this.paymentRepository = paymentRepository;
        this.redisTemplate = redisTemplate;
    }

    // =========================
    // CREATE PAYMENT
    // =========================

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {

        String idempotencyKey = UUID.randomUUID().toString();
        request.setIdempotencyKey(idempotencyKey);
        // 1. Verificar cache no Redis
        String redisKey = "payment:" + idempotencyKey;

        Object cachedResponse =
                redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {

            return (PaymentResponse) cachedResponse;
        }

        // 2. Idempotência no banco
        validateIdempotency(idempotencyKey);

        // 2. Buscar usuários
        User payer = findUser(request.getPayerId());
        User payee = findUser(request.getPayeeId());

        // 3. Regras de negócio
        if (request.getPayerId().equals(request.getPayeeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Transferência inválida");
        }

        if (payer.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        Payment payment = PaymentFactory.fromRequest(request, Enum_Payment.PENDING);

        // Persiste em transação separada (REQUIRES_NEW) para que o fraud-service
        // consiga enxergar o registro quando fizer GET /payments/{id}.
        payment = paymentPersistenceHelper.saveInNewTx(payment);

        // TODO design pattern strategy de acordo com cada status devolvido pelo antifraud
        try {
            FraudAnalysisRequest analysisRequest = AnalysisRequestBuilder.fromAnalysisRequest(payment);

            FraudAnalysisResponse analysisResponse = antiFraudClient.analyzeTransaction(analysisRequest).getBody();

            if (analysisResponse == null){
                throw new RuntimeException("Resposta do serviço de fraude nula");
            }

            PaymentStatusHandler handler = handlerFactory.getHandler(analysisResponse.getStatus());
            handler.handle(payment, analysisResponse);


        } catch (ResponseStatusException ex) {
            paymentPersistenceHelper.updateStatusInNewTx(payment, Enum_Payment.FAILED);
            throw ex;
        }

        PaymentResponse response =
                PaymentResponseFactory.fromPayment(payment);

        // salva no Redis por 24h
        redisTemplate.opsForValue().set(
                redisKey,
                response,
                java.time.Duration.ofHours(24)
        );

        return response;
    }

    // =========================
    // HELPERS
    // =========================

    private void validateIdempotency(String key) {
        if (paymentRepository.findByIdempotencyKey(key).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Pagamento já processado");
        }
    }

    private void authorizePayment(Payment payment) {

        FraudAnalysisResponse analysisResponse = antiFraudClient.analyzeTransaction(AnalysisRequestBuilder.fromAnalysisRequest(payment)).getBody();

        assert analysisResponse != null;
        if (analysisResponse.getStatus().equals(REJECTED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pagamento não autorizado");
        }
    }

    private User findUser(UUID id) {
        return userRepository.findByUuidForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public PaymentResponse getById(UUID id) {
        Payment payment = paymentRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        return PaymentResponseFactory.fromPayment(payment);
    }

    public List<PaymentResponse> getByUser(UUID userId) {

        List<Payment> payments =
                paymentRepository.findByPayerIdOrPayeeId(userId, userId);

        return payments.stream()
                .map(PaymentResponseFactory::fromPayment)
                .toList();
    }

    public List<PaymentResponse> getByStatus(Enum_Payment status) {
        List<Payment> payments = paymentRepository.findByStatus(status);
        return payments.stream()
                .map(PaymentResponseFactory::fromPayment)
                .toList();
    }

    public List<PaymentResponse> getAll() {
        List<Payment> payments = paymentRepository.findAll();
        return payments.stream()
                .map(PaymentResponseFactory::fromPayment)
                .toList();
    }

    @Transactional
    public void approveManualPayment(UUID paymentId, String reason){
        log.info("Aprovando pagamento manual: PaymentId={}, Reason={}", paymentId, reason);

        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        User payer = findUser(payment.getPayerId());
        if (payer.getBalance().compareTo(payment.getAmount()) < 0){
            paymentPersistenceHelper.updateStatusInNewTx(payment, Enum_Payment.FAILED);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        payer.setBalance(payer.getBalance().subtract(payment.getAmount()));
        User payee = findUser(payment.getPayeeId());
        payee.setBalance(payee.getBalance().add(payment.getAmount()));

        userRepository.save(payer);
        userRepository.save(payee);

        payment.setStatus(Enum_Payment.SUCCESS);
        paymentRepository.save(payment);

        log.info("Pagamento aprovado com sucesso: PaymentId={}" ,paymentId);
    }

    @Transactional
    public void rejectManualPayment(UUID paymentId, String reason){
        log.info("Rejeitando pagamento manual: PaymentId={}, Reason={}" ,paymentId, reason);

        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        payment.setStatus(Enum_Payment.REJECTED);
        paymentRepository.save(payment);

        log.info("Pagamento rejeitado: PaymentId={}" ,paymentId);
    }
}
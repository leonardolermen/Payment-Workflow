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
import com.payflow.coreservice.repository.PaymentRepository;
import lombok.Data;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import com.payflow.coreservice.strategy.factory.PaymentStatusHandlerFactory;

import java.util.List;
import java.util.UUID;

@Data
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AntiFraudClient antiFraudClient;
    private final PaymentPersistenceHelper paymentPersistenceHelper;
    private final PaymentStatusHandlerFactory handlerFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          AntiFraudClient antiFraudClient,
                          PaymentPersistenceHelper paymentPersistenceHelper,
                          PaymentStatusHandlerFactory handlerFactory,
                          RedisTemplate<String, Object> redisTemplate) {
        this.antiFraudClient = antiFraudClient;
        this.paymentPersistenceHelper = paymentPersistenceHelper;
        this.handlerFactory = handlerFactory;
        this.paymentRepository = paymentRepository;
        this.redisTemplate = redisTemplate;
    }

    // =========================
    // CREATE PAYMENT
    // =========================

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

        Payment payment = paymentPersistenceHelper.createPendingPayment(request);

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
}
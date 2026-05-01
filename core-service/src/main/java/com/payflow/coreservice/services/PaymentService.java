package com.payflow.coreservice.services;

import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.client.AntiFraudClient;
import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.dto.factory.PaymentResponseFactory;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.factory.PaymentFactory;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.payflow.commons.enums.fraud.Status_Fraud.REJECTED;
import static java.util.Objects.isNull;
import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Data
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AntiFraudClient antiFraudClient;
    private final PaymentPersistenceHelper paymentPersistenceHelper;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          AntiFraudClient antiFraudClient,
                          PaymentPersistenceHelper paymentPersistenceHelper) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.antiFraudClient = antiFraudClient;
        this.paymentPersistenceHelper = paymentPersistenceHelper;
    }

    // =========================
    // CREATE PAYMENT
    // =========================

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO dto) {

        // 1. Idempotência
        validateIdempotency(dto.getIdempotencyKey());

        // 2. Buscar usuários
        User payer = findUser(dto.getPayerId());
        User payee = findUser(dto.getPayeeId());

        // 3. Regras de negócio
        if (dto.getPayerId().equals(dto.getPayeeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Transferência inválida");
        }

        if (payer.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        Payment payment = PaymentFactory.fromRequest(dto, Enum_Payment.PENDING);

        // Persiste em transação separada (REQUIRES_NEW) para que o fraud-service
        // consiga enxergar o registro quando fizer GET /payments/{id}.
        payment = paymentPersistenceHelper.saveInNewTx(payment);

        // 4. Fraud (simulado)
        // TODO design pattern strategy de acordo com cada status devolvido pelo antifraud
        try {
            authorizePayment(payment);
        } catch (ResponseStatusException ex) {
            paymentPersistenceHelper.updateStatusInNewTx(payment, Enum_Payment.FAILED);
            throw ex;
        }

        payer.setBalance(payer.getBalance().subtract(payment.getAmount()));
        payee.setBalance(payee.getBalance().add(payment.getAmount()));

        userRepository.save(payer);
        userRepository.save(payee);

        payment.setStatus(Enum_Payment.SUCCESS);
        paymentRepository.save(payment);

        return PaymentResponseFactory.fromPayment(payment);
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

        FraudAnalysisRequest analysisRequest = FraudAnalysisRequest.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .build();

        FraudAnalysisResponse analysisResponse = antiFraudClient.analyzeTransaction(analysisRequest).getBody();

        assert analysisResponse != null;
        if (analysisResponse.getStatus().equals(REJECTED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pagamento não autorizado");
        }
    }

    private User findUser(UUID id) {
        return userRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public PaymentResponse getById(UUID id) {
        Payment payment = paymentRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        return PaymentResponseFactory.fromPayment(payment);
    }

    public List<PaymentResponseDTO> getByUser(UUID userId) {

        List<Payment> payments =
                paymentRepository.findByPayerIdOrPayeeId(userId, userId);

        return payments.stream()
                .map(PaymentResponseFactory::fromPayment)
                .toList();
    }
}
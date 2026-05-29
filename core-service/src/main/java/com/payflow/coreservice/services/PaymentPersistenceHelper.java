package com.payflow.coreservice.services;

import com.payflow.commons.dto.payment.PaymentRequest;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.model.factory.PaymentFactory;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Helper para persistência de Payment em transações independentes.
 * Usado para garantir que o pagamento PENDING seja comitado antes da
 * chamada externa ao fraud-service (que consulta o DB via HTTP).
 */
@Component
public class PaymentPersistenceHelper {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PaymentPersistenceHelper(PaymentRepository paymentRepository,
                                    UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPendingPayment(PaymentRequest request) {
        if (request.getIdempotencyKey() == null) {
            throw new IllegalArgumentException("Idempotency key must be set before persisting payment");
        }

        paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento já processado");
                });

        User payer = userRepository.findByUuidForUpdate(request.getPayerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário pagador não encontrado"));

        User payee = userRepository.findByUuidForUpdate(request.getPayeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário recebedor não encontrado"));

        if (request.getPayerId().equals(request.getPayeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transferência inválida");
        }

        if (payer.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        payer.setBalance(payer.getBalance().subtract(request.getAmount()));
        payee.setBalance(payee.getBalance().add(request.getAmount()));

        Payment payment = PaymentFactory.fromRequest(request, Enum_Payment.PENDING,
                builder -> builder
                        .payerId(payer.getUuid())
                        .payeeId(payee.getUuid()));

        userRepository.save(payer);
        userRepository.save(payee);

        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment saveInNewTx(Payment payment) {
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusInNewTx(Payment payment, Enum_Payment status) {
        payment.setStatus(status);
        paymentRepository.saveAndFlush(payment);
    }
}

package com.payflow.coreservice.services;

import com.payflow.coreservice.enums.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper para persistência de Payment em transações independentes.
 * Usado para garantir que o pagamento PENDING seja comitado antes da
 * chamada externa ao fraud-service (que consulta o DB via HTTP).
 */
@Component
public class PaymentPersistenceHelper {

    private final PaymentRepository paymentRepository;

    public PaymentPersistenceHelper(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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

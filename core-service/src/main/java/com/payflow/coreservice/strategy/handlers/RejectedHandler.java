package com.payflow.coreservice.strategy.handlers;

import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RejectedHandler implements PaymentStatusHandler {

    private static final Logger logger = LoggerFactory.getLogger(RejectedHandler.class);
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void handle(Payment payment, FraudAnalysisResponse response){
        payment.setStatus(Enum_Payment.FAILED);
        paymentRepository.save(payment);

        logger.warn("Pagamento rejeitado pelo anti-fraude: " +
                        "UUID: {}, Payer: {}, Payee: {}, Amount: {}, Status: {}",
                payment.getUuid(),
                payment.getPayerId(),
                payment.getPayeeId(),
                payment.getAmount(),
                response.getStatus());
    }
}

package com.payflow.coreservice.dto.factory;

import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.model.Payment;

public final class PaymentResponseFactory {

    private PaymentResponseFactory() {
    }

    public static PaymentResponseDTO fromPayment(Payment payment) {
        return PaymentResponseDTO.builder()
               .id(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
               .build();
    }
}

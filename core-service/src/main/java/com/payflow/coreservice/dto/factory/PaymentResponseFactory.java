package com.payflow.coreservice.dto.factory;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.model.Payment;

public final class PaymentResponseFactory {

    private PaymentResponseFactory() {
    }

    public static PaymentResponse fromPayment(Payment payment) {
        return PaymentResponse.builder()
               .id(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
               .build();
    }
}

package com.payflow.coreservice.dto.factory;

import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.model.Payment;

public final class PaymentResponseFactory {

    private PaymentResponseFactory() {
    }

    public static PaymentResponseDTO fromPayment(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}

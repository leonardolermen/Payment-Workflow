package com.payflow.coreservice.model.factory;

import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.enums.Enum_Payment;
import com.payflow.coreservice.model.Payment;

import java.util.function.Function;

public final class PaymentFactory {

    private PaymentFactory() {
    }

    public static Payment fromRequest(
            PaymentRequestDTO request,
            Enum_Payment status,
            Function<Payment.PaymentBuilder, Payment.PaymentBuilder> customizer
    ) {
        Payment.PaymentBuilder builder = Payment.builder()
                .payerId(request.getPayerId())
                .payeeId(request.getPayeeId())
                .amount(request.getAmount())
                .status(status)
                .idempotencyKey(request.getIdempotencyKey());

        if (customizer != null) {
            builder = customizer.apply(builder);
        }

        return builder.build();
    }

    public static Payment fromRequest(PaymentRequestDTO request, Enum_Payment status) {
        return fromRequest(request, status, null);
    }
}

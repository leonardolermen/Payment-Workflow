package com.payflow.coreservice.builder;

import com.payflow.coreservice.dto.factory.PaymentDetailsRequest;
import com.payflow.coreservice.model.Payment;

public class PaymentDetailsBuilder {

    public static PaymentDetailsRequest fromDetails(Payment payment){
        return PaymentDetailsRequest.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

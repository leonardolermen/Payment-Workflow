package com.payflow.coreservice.builder;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.alert.AlertType;
import com.payflow.coreservice.model.Payment;

public class PaymentAlertEventBuilder {

    public static PaymentAlertEvent fromAlertEvento(Payment payment, FraudAnalysisResponse response){
        return PaymentAlertEvent.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .alertType(AlertType.valueOf(response.getStatus().name()))
                .reason(response.getReason())
                .timestamp(payment.getCreatedAt())
                .build();
    }
}

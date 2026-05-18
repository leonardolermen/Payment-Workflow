package com.payflow.coreservice.builder;

import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.coreservice.model.Payment;

public class AnalysisRequestBuilder {

    public static FraudAnalysisRequest fromAnalysisRequest(Payment payment){
        return FraudAnalysisRequest.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .build();
    }
}

package com.payflow.fraudservice.builder;

import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.fraud.Status_Fraud;

import java.util.UUID;

public class FraudAnalysisResponseBuilder {

    public static FraudAnalysisResponse fromAnalysis(Status_Fraud  status, double score, String reason, UUID paymentId) {
        return FraudAnalysisResponse.builder()
                .status(status)
                .score(score)
                .reason(reason)
                .paymentId(paymentId)
                .build();
    }

    public static FraudAnalysisResponse fromInsufficientBalance(UUID paymentId) {
        return FraudAnalysisResponse.builder()
                .status(Status_Fraud.REJECTED)
                .score(100.0)
                .reason("Saldo insuficiente")
                .paymentId(paymentId)
                .build();
    }
}
package com.payflow.coreservice.builder;

import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.fraud.Status_Fraud;

public class DecisionBuilder {

    private DecisionBuilder (){

    }

    public DecisionBuilder from FraudAnalysisResponse
}


FraudAnalysisResponse mockResponse = FraudAnalysisResponse.builder()
        .paymentId(decision.getPaymentId())
        .status(decision.getDecision().equals("APPROVED") ?
                Status_Fraud.APPROVED : Status_Fraud.REJECTED)
        .reason(decision.getReason())
        .build();
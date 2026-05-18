package com.payflow.coreservice.builder;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.fraud.Status_Fraud;


public class DecisionBuilder {

    public static FraudAnalysisResponse fromDecision(ManualReviewDecision decision){
        Status_Fraud status = decision.getDecision().equals("APPROVED") ?
                Status_Fraud.APPROVED : Status_Fraud.REJECTED;

        return FraudAnalysisResponse.builder()
                .paymentId(decision.getPaymentId())
                .status(status)
                .reason(decision.getReason())
                .build();
    }

}


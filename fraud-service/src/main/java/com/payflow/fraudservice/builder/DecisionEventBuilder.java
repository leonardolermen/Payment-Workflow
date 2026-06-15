package com.payflow.fraudservice.builder;

import com.payflow.commons.dto.alert.ManualReviewDecision;

import java.util.UUID;

public class DecisionEventBuilder {

    public static ManualReviewDecision build(UUID paymentId, String decision,
                                             String reviewerId, String reason){
        return ManualReviewDecision.builder()
                .paymentId(paymentId)
                .decision(decision)
                .reviewerId(reviewerId)
                .reason(reason)
                .build();
    }
}

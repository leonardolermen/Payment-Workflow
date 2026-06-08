package com.payflow.fraudservice.builder;


import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.fraudservice.model.FraudAnalysisLog;

import java.time.LocalDateTime;
import java.util.UUID;


public class FraudAnalysisLogBuilder {


    public static FraudAnalysisLog build(UUID paymentId, double score,
                                         Status_Fraud status, String reason) {
        FraudAnalysisLog log = new FraudAnalysisLog();
        log.setPaymentId(paymentId);
        log.setScore(score);
        log.setStatus(status);
        log.setReason(reason);
        log.setEvaluatedAt(LocalDateTime.now());
        return log;
    }

    public static FraudAnalysisLog buildInsufficientBalance(UUID paymentId) {
        FraudAnalysisLog log = new FraudAnalysisLog();
        log.setPaymentId(paymentId);
        log.setScore(100.0);
        log.setStatus(Status_Fraud.REJECTED);
        log.setReason("Saldo insuficiente");
        log.setEvaluatedAt(LocalDateTime.now());
        return log;
    }
}


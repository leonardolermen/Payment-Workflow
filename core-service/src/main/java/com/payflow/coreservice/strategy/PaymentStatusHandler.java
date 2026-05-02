package com.payflow.coreservice.strategy;

import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.coreservice.model.Payment;

public interface PaymentStatusHandler {
    void handle(Payment payment, FraudAnalysisResponse response);
}

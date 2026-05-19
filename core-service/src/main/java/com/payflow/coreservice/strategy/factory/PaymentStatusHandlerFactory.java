package com.payflow.coreservice.strategy.factory;

import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import com.payflow.coreservice.strategy.handlers.paymen.status.handler.ApprovedHandler;
import com.payflow.coreservice.strategy.handlers.paymen.status.handler.ManualAnalysisHandler;
import com.payflow.coreservice.strategy.handlers.paymen.status.handler.PendingReviewHandler;
import com.payflow.coreservice.strategy.handlers.paymen.status.handler.RejectedHandler;
import com.payflow.coreservice.strategy.handlers.paymen.status.handler.SuspiciousHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentStatusHandlerFactory {

    private final Map<Status_Fraud, PaymentStatusHandler> handlers;

    public PaymentStatusHandlerFactory(
            ApprovedHandler approvedHandler,
            RejectedHandler rejectedHandler,
            PendingReviewHandler pendingReviewHandler,
            ManualAnalysisHandler manualAnalysisHandler,
            SuspiciousHandler suspiciousHandler){

        this.handlers = Map.of(
                Status_Fraud.APPROVED, approvedHandler,
                Status_Fraud.REJECTED, rejectedHandler,
                Status_Fraud.PENDING_REVIEW, pendingReviewHandler,
                Status_Fraud.MANUAL_ANALYSIS, manualAnalysisHandler,
                Status_Fraud.SUSPICIOUS, suspiciousHandler
        );
    }

    public PaymentStatusHandler getHandler(Status_Fraud status) {
        PaymentStatusHandler handler = handlers.get(status);
        if (handler == null) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }

        return handler;
    }
}

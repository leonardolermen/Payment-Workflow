package com.payflow.coreservice.strategy.factory;

import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import com.payflow.coreservice.strategy.handlers.ApprovedHandler;
import com.payflow.coreservice.strategy.handlers.RejectedHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentStatusHandlerFactory {

    private final Map<Status_Fraud, PaymentStatusHandler> handlers;

    public PaymentStatusHandlerFactory(
            ApprovedHandler approvedHandler,
            RejectedHandler rejectedHandler){

        this.handlers = Map.of(
                Status_Fraud.APPROVED, approvedHandler,
                Status_Fraud.REJECTED, rejectedHandler
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

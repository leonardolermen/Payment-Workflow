package com.payflow.coreservice.strategy.handlers.alert.factory;

import com.payflow.commons.enums.alert.AlertType;
import com.payflow.coreservice.strategy.handlers.alert.AlertHandler;
import com.payflow.coreservice.strategy.handlers.alert.impl.ManualAnalysisHandler;
import com.payflow.coreservice.strategy.handlers.alert.impl.PendingReviewHandler;
import com.payflow.coreservice.strategy.handlers.alert.impl.SuspiciousHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AlertHandlerFactory {

    private final Map<AlertType, AlertHandler> handlers;

    public AlertHandlerFactory(
            PendingReviewHandler pendingReviewHandler,
            ManualAnalysisHandler manualAnalysisHandler,
            SuspiciousHandler suspiciousHandler) {

        this.handlers = Map.of(
                AlertType.PENDING_REVIEW, pendingReviewHandler,
                AlertType.MANUAL_ANALYSIS, manualAnalysisHandler,
                AlertType.SUSPICIOUS, suspiciousHandler
        );
    }

    public AlertHandler getHandler(AlertType type) {
        AlertHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Tipo de alerta inválido: " + type);
        }
        return handler;
    }
}

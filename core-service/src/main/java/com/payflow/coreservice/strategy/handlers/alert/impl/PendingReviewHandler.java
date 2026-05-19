package com.payflow.coreservice.strategy.handlers.alert.impl;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.enums.alert.AlertType;
import com.payflow.coreservice.strategy.handlers.alert.AlertHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PendingReviewHandler implements AlertHandler {

    private static final Logger logger = LoggerFactory.getLogger(PendingReviewHandler.class);

    @Override
    public void handle(PaymentAlertEvent alert) {
        logger.info("Enviando email para equipe de análise: {} | Pagamento: {}",
                alert.getAlertType(), alert.getPaymentId());
    }

    @Override
    public AlertType getSupportedType() {
        return AlertType.PENDING_REVIEW;
    }
}

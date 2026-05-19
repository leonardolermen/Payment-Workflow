package com.payflow.coreservice.strategy.handlers.alert.impl;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.enums.alert.AlertType;
import com.payflow.coreservice.strategy.handlers.alert.AlertHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("suspiciousAlertHandler")
public class SuspiciousHandler implements AlertHandler {

    private static final Logger logger = LoggerFactory.getLogger(SuspiciousHandler.class);

    @Override
    public void handle(PaymentAlertEvent alert) {
        logger.info("Enviando email para equipe de segurança: {} | Pagamento: {}",
                alert.getAlertType(), alert.getPaymentId());
    }

    @Override
    public AlertType getSupportedType() {
        return AlertType.SUSPICIOUS;
    }
}

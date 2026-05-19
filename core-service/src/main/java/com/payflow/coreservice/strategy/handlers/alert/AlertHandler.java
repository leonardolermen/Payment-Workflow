package com.payflow.coreservice.strategy.handlers.alert;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.enums.alert.AlertType;

public interface AlertHandler {

    void handle(PaymentAlertEvent alert);

    AlertType getSupportedType();
}

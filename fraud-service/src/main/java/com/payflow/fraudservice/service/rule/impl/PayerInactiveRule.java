package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.commons.enums.user.User_Status;
import com.payflow.fraudservice.service.rule.RiskRule;
import org.springframework.stereotype.Component;

@Component
public class PayerInactiveRule implements RiskRule {

    @Override
    public String code() {
        return "PAYER_INACTIVE";
    }

    @Override
    public int weight() {
        return 40;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return payer.getStatus() != User_Status.ACTIVE;
    }
}

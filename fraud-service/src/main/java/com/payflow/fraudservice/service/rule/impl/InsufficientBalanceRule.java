package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import org.springframework.stereotype.Component;

@Component
public class InsufficientBalanceRule implements RiskRule {

    @Override
    public String code() {
        return "INSUFFICIENT_BALANCE";
    }

    @Override
    public int weight() {
        return 30;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return payer.getBalance().compareTo(payment.getAmount()) < 0;
    }
}

package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighValueRule implements RiskRule {

    private static final BigDecimal LIMIT = new BigDecimal("25000");

    @Override
    public String code() {
        return "HIGH_VALUE";
    }

    @Override
    public int weight() {
        return 30;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return payment.getAmount().compareTo(LIMIT) > 0;
    }
}

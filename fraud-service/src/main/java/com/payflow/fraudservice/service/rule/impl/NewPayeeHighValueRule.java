package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class NewPayeeHighValueRule implements RiskRule {

    private static final BigDecimal LIMIT = new BigDecimal("35000");

    @Override
    public String code() {
        return "NEW_PAYEE_HIGH_VALUE";
    }

    @Override
    public int weight() {
        return 70;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return payee.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))
                && payment.getAmount().compareTo(LIMIT) > 0;
    }
}

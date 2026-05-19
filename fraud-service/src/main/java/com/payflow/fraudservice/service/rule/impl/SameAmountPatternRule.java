package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import com.payflow.fraudservice.service.rule.TransactionHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SameAmountPatternRule implements RiskRule {

    private static final int SAME_AMOUNT_LIMIT = 3;
    private static final int HOURS_WINDOW = 24;

    @Override
    public String code() {
        return "SAME_AMOUNT_PATTERN";
    }

    @Override
    public int weight() {
        return 60;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return false;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee, TransactionHistory history) {
        long sameAmountCount = history.transactions().stream()
                .filter(t -> t.timestamp().plusHours(HOURS_WINDOW).isAfter(java.time.LocalDateTime.now()))
                .filter(t -> t.amount().compareTo(payment.getAmount()) == 0)
                .count();

        return sameAmountCount >= SAME_AMOUNT_LIMIT;
    }
}

package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import com.payflow.fraudservice.service.rule.TransactionHistory;
import org.springframework.stereotype.Component;

@Component
public class VelocityRule implements RiskRule {

    private static final int HOURLY_LIMIT = 10;
    private static final int DAILY_LIMIT = 30;

    @Override
    public String code() {
        return "VELOCITY";
    }

    @Override
    public int weight() {
        return 50;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return false;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee, TransactionHistory history) {
        long hourlyCount = history.transactions().stream()
                .filter(t -> t.timestamp().plusHours(1).isAfter(java.time.LocalDateTime.now()))
                .count();

        long dailyCount = history.transactions().stream()
                .filter(t -> t.timestamp().plusHours(24).isAfter(java.time.LocalDateTime.now()))
                .count();

        return hourlyCount >= HOURLY_LIMIT || dailyCount >= DAILY_LIMIT;
    }
}

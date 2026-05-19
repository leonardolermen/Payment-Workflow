package com.payflow.fraudservice.service.rule.impl;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.service.rule.RiskRule;
import com.payflow.fraudservice.service.rule.TransactionHistory;
import org.springframework.stereotype.Component;

@Component
public class RapidSuccessivePaymentsRule implements RiskRule {

    private static final int RAPID_LIMIT = 5;
    private static final int MINUTES_WINDOW = 5;

    @Override
    public String code() {
        return "RAPID_SUCCESSIVE_PAYMENTS";
    }

    @Override
    public int weight() {
        return 55;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return false;
    }

    @Override
    public boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee, TransactionHistory history) {
        long rapidCount = history.transactions().stream()
                .filter(t -> t.timestamp().plusMinutes(MINUTES_WINDOW).isAfter(java.time.LocalDateTime.now()))
                .count();

        return rapidCount >= RAPID_LIMIT;
    }
}

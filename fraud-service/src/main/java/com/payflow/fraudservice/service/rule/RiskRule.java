package com.payflow.fraudservice.service.rule;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;

public interface RiskRule {

    String code();

    int weight();

    boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee);

    default boolean matches(PaymentResponse payment, UserResponse payer, UserResponse payee, TransactionHistory history) {
        return matches(payment, payer, payee);
    }
}

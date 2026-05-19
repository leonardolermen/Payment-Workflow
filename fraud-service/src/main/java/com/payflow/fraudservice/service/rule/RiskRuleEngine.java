package com.payflow.fraudservice.service.rule;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RiskRuleEngine {

    private static final int REJECTION_THRESHOLD = 70;
    private static final int MAX_SCORE = 100;

    private final List<RiskRule> rules;

    public RiskRuleEngine(List<RiskRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(RiskRule::weight).reversed())
                .toList();
    }

    public RiskResult evaluate(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        return evaluate(payment, payer, payee, null);
    }

    public RiskResult evaluate(PaymentResponse payment, UserResponse payer, UserResponse payee, TransactionHistory history) {
        int score = 0;
        List<String> triggeredRules = new ArrayList<>();

        for (RiskRule rule : rules) {
            boolean matches;
            if (history != null) {
                matches = rule.matches(payment, payer, payee, history);
            } else {
                matches = rule.matches(payment, payer, payee);
            }

            if (!matches) continue;

            score += rule.weight();
            triggeredRules.add(rule.code());

            if (score >= REJECTION_THRESHOLD) break;
        }

        return new RiskResult(Math.min(score, MAX_SCORE), triggeredRules);
    }
}

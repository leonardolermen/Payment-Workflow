package com.payflow.fraudservice.service.rule;

import java.util.List;

public record RiskResult(int score, List<String> triggeredRules) {

    public boolean isRejected(int threshold) {
        return score >= threshold;
    }

    public boolean hasTriggered(String ruleCode) {
        return triggeredRules.contains(ruleCode);
    }
}

package com.payflow.fraudservice.traceflow;

import com.traceflow.sdk.SensitiveFieldFilter;
import com.traceflow.sdk.TraceFlow;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AOP aspect that automatically logs fraud analysis business events
 * into the active TraceFlow span — no changes to FraudAnalysisService required.
 */
@Aspect
@Component
@ConditionalOnClass(name = "com.traceflow.sdk.TraceFlow")
public class FraudAnalysisAspect {

    // ─── analyzePayment ───────────────────────────────────────────────────────

    @Around("execution(* com.payflow.fraudservice.service.FraudAnalysisService.analyzePayment(..))")
    public Object aroundAnalyzePayment(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        // Log the incoming request
        if (args.length > 0 && args[0] != null) {
            Map<String, String> reqAttrs = SensitiveFieldFilter.fromObject(args[0]);
            TraceFlow.log("fraud.analysis.started", reqAttrs);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (result != null) {
                Map<String, String> attrs = new LinkedHashMap<>(SensitiveFieldFilter.fromObject(result));
                attrs.put("elapsed_ms", String.valueOf(elapsed));

                // Determine level based on fraud status
                Object statusField = getFieldSafe(result, "status");
                String status = statusField != null ? statusField.toString() : "";

                if (status.contains("REJECTED")) {
                    TraceFlow.error("fraud.analysis.decision", attrs);
                } else if (status.contains("MANUAL_ANALYSIS")) {
                    TraceFlow.warn("fraud.analysis.decision", attrs);
                } else {
                    TraceFlow.log("fraud.analysis.decision", attrs);
                }
            }

            return result;

        } catch (Throwable ex) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("error.type", ex.getClass().getSimpleName());
            attrs.put("error.message", ex.getMessage() != null ? ex.getMessage() : "unknown");
            attrs.put("elapsed_ms", String.valueOf(System.currentTimeMillis() - start));
            TraceFlow.error("fraud.analysis.failed", attrs);
            throw ex;
        }
    }

    // ─── RiskRuleEngine.evaluate ──────────────────────────────────────────────

    @Around("execution(* com.payflow.fraudservice.service.rule.RiskRuleEngine.evaluate(..))")
    public Object aroundRuleEngine(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        if (result != null) {
            Map<String, String> attrs = new LinkedHashMap<>();
            Object score = getFieldSafe(result, "score");
            Object rules = getFieldSafe(result, "triggeredRules");
            if (score != null) attrs.put("risk_score", score.toString());
            if (rules != null) attrs.put("triggered_rules", rules.toString());

            // Score thresholds: ≥70 REJECTED, ≥30 MANUAL_ANALYSIS, <30 APPROVED
            double scoreVal = score != null ? parseDouble(score.toString()) : 0;
            if (scoreVal >= 70) {
                TraceFlow.error("fraud.rules.evaluated", attrs);
            } else if (scoreVal >= 30) {
                TraceFlow.warn("fraud.rules.evaluated", attrs);
            } else {
                TraceFlow.log("fraud.rules.evaluated", attrs);
            }
        }

        return result;
    }

    // ─── Individual rule evaluations ──────────────────────────────────────────

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.HighValueRule.matches(..))")
    public Object aroundHighValueRule(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        boolean triggered = Boolean.TRUE.equals(result);
        if (triggered) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "HIGH_VALUE_TRANSACTION", "weight", "30"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.VelocityRule.matches(..))")
    public Object aroundVelocityRule(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "VELOCITY_LIMIT_EXCEEDED", "weight", "25"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.RapidSuccessivePaymentsRule.matches(..))")
    public Object aroundRapidPaymentsRule(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "RAPID_SUCCESSIVE_PAYMENTS", "weight", "20"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.SameAmountPatternRule.matches(..))")
    public Object aroundSameAmountRule(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "SAME_AMOUNT_PATTERN", "weight", "15"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.NewPayeeHighValueRule.matches(..))")
    public Object aroundNewPayeeRule(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "NEW_PAYEE_HIGH_VALUE", "weight", "20"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.InsufficientBalanceRule.matches(..))")
    public Object aroundInsufficientBalance(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.warn("fraud.rule.triggered", Map.of("rule", "INSUFFICIENT_BALANCE", "weight", "40"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.PayerInactiveRule.matches(..))")
    public Object aroundPayerInactive(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.error("fraud.rule.triggered", Map.of("rule", "PAYER_INACTIVE_ACCOUNT", "weight", "50"));
        }
        return result;
    }

    @Around("execution(* com.payflow.fraudservice.service.rule.impl.PayeeInactiveRule.matches(..))")
    public Object aroundPayeeInactive(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (Boolean.TRUE.equals(result)) {
            TraceFlow.error("fraud.rule.triggered", Map.of("rule", "PAYEE_INACTIVE_ACCOUNT", "weight", "50"));
        }
        return result;
    }

    // ─── TransactionHistoryCacheService ───────────────────────────────────────

    @Around("execution(* com.payflow.fraudservice.service.cache.TransactionHistoryCacheService.getUserTransactions(..))")
    public Object aroundGetHistory(ProceedingJoinPoint pjp) throws Throwable {
        Object userId = pjp.getArgs().length > 0 ? pjp.getArgs()[0] : "unknown";
        Object result = pjp.proceed();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("user_id", String.valueOf(userId));
        if (result instanceof java.util.Collection<?> col) {
            attrs.put("history_count", String.valueOf(col.size()));
        }
        TraceFlow.debug("fraud.history.loaded", attrs);
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Object getFieldSafe(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}

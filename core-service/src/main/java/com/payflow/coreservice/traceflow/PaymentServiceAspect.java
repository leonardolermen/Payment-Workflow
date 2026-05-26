package com.payflow.coreservice.traceflow;

import com.traceflow.sdk.SensitiveFieldFilter;
import com.traceflow.sdk.TraceContext;
import com.traceflow.sdk.TraceFlow;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AOP aspect that automatically logs business events from PaymentService
 * into the active TraceFlow span — no changes to PaymentService required.
 *
 * Activated automatically when traceflow-spring-boot-starter is on the classpath.
 */
@Aspect
@Component
@ConditionalOnClass(name = "com.traceflow.sdk.TraceFlow")
public class PaymentServiceAspect {

    // ─── createPayment ────────────────────────────────────────────────────────

    @Around("execution(* com.payflow.coreservice.services.PaymentService.createPayment(..))")
    public Object aroundCreatePayment(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] != null) {
            Map<String, String> attrs = SensitiveFieldFilter.fromObject(args[0]);
            TraceFlow.log("payment.request.received", attrs);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;

            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("elapsed_ms", String.valueOf(elapsed));
            if (result != null) {
                Map<String, String> resultAttrs = SensitiveFieldFilter.fromObject(result);
                attrs.putAll(resultAttrs);
            }
            TraceFlow.log("payment.created.success", attrs);
            return result;

        } catch (Throwable ex) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("error.type", ex.getClass().getSimpleName());
            attrs.put("error.message", ex.getMessage() != null ? ex.getMessage() : "unknown");
            attrs.put("elapsed_ms", String.valueOf(System.currentTimeMillis() - start));
            TraceFlow.error("payment.created.failed", attrs);
            throw ex;
        }
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Around("execution(* com.payflow.coreservice.services.PaymentService.getById(..))")
    public Object aroundGetById(ProceedingJoinPoint pjp) throws Throwable {
        Object id = pjp.getArgs().length > 0 ? pjp.getArgs()[0] : null;
        TraceFlow.debug("payment.lookup.by_id", Map.of("payment_id", String.valueOf(id)));
        return pjp.proceed();
    }

    // ─── PaymentPersistenceHelper ─────────────────────────────────────────────

    @Around("execution(* com.payflow.coreservice.services.PaymentPersistenceHelper.saveInNewTx(..))")
    public Object aroundSavePayment(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (result != null) {
            Map<String, String> attrs = SensitiveFieldFilter.fromObject(result);
            TraceFlow.log("payment.persisted", attrs);
        }
        return result;
    }

    @Around("execution(* com.payflow.coreservice.services.PaymentPersistenceHelper.updateStatusInNewTx(..))")
    public Object aroundUpdateStatus(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Map<String, String> attrs = new LinkedHashMap<>();
        if (args.length > 0 && args[0] != null) attrs.put("payment_id", String.valueOf(
                getFieldSafe(args[0], "uuid")));
        if (args.length > 1 && args[1] != null) attrs.put("new_status", String.valueOf(args[1]));
        TraceFlow.log("payment.status.updated", attrs);
        return pjp.proceed();
    }

    // ─── Strategy Handlers ────────────────────────────────────────────────────

    @Around("execution(* com.payflow.coreservice.strategy.handlers.paymen.status.handler.ApprovedHandler.handle(..))")
    public Object aroundApproved(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("handler", "ApprovedHandler");
        if (args.length > 0 && args[0] != null) {
            attrs.put("payment_id",   String.valueOf(getFieldSafe(args[0], "uuid")));
            attrs.put("amount",       String.valueOf(getFieldSafe(args[0], "amount")));
            attrs.put("payer_id",     String.valueOf(getFieldSafe(args[0], "payerId")));
            attrs.put("payee_id",     String.valueOf(getFieldSafe(args[0], "payeeId")));
        }
        if (args.length > 1 && args[1] != null) {
            attrs.put("fraud_score",  String.valueOf(getFieldSafe(args[1], "score")));
            attrs.put("fraud_reason", String.valueOf(getFieldSafe(args[1], "reason")));
        }
        TraceFlow.log("payment.strategy.approved", attrs);

        Object result = pjp.proceed();
        TraceFlow.log("payment.balance.transferred", Map.of(
                "payment_id", attrs.getOrDefault("payment_id", ""),
                "amount",     attrs.getOrDefault("amount", "")
        ));
        return result;
    }

    @Around("execution(* com.payflow.coreservice.strategy.handlers.paymen.status.handler.RejectedHandler.handle(..))")
    public Object aroundRejected(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("handler", "RejectedHandler");
        if (args.length > 0 && args[0] != null) {
            attrs.put("payment_id", String.valueOf(getFieldSafe(args[0], "uuid")));
            attrs.put("amount",     String.valueOf(getFieldSafe(args[0], "amount")));
        }
        if (args.length > 1 && args[1] != null) {
            attrs.put("fraud_score",  String.valueOf(getFieldSafe(args[1], "score")));
            attrs.put("fraud_reason", String.valueOf(getFieldSafe(args[1], "reason")));
            attrs.put("fraud_status", String.valueOf(getFieldSafe(args[1], "status")));
        }
        TraceFlow.error("payment.strategy.rejected", attrs);
        return pjp.proceed();
    }

    @Around("execution(* com.payflow.coreservice.strategy.handlers.paymen.status.handler.ManualAnalysisHandler.handle(..))")
    public Object aroundManualAnalysis(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("handler", "ManualAnalysisHandler");
        if (args.length > 1 && args[1] != null) {
            attrs.put("fraud_score", String.valueOf(getFieldSafe(args[1], "score")));
            attrs.put("reason",      String.valueOf(getFieldSafe(args[1], "reason")));
        }
        TraceFlow.warn("payment.strategy.manual_analysis", attrs);
        return pjp.proceed();
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
        return "";
    }
}

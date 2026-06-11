package com.payflow.coreservice.builder;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.dto.webhook.WebhookEvent;
import com.payflow.commons.enums.webhook.WebhookType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WebhookEventBuilder {
    public static WebhookEvent buildFraudTeamAlert(PaymentAlertEvent alert){
        return WebhookEvent.builder()
                .paymentId(alert.getPaymentId())
                .payeeId(alert.getPayeeId())
                .payerId(alert.getPayerId())
                .amount(alert.getAmount())
                .webhookType(WebhookType.FRAUD_TEAM_ALERT)
                .status(alert.getAlertType().name())
                .reason(alert.getReason())
                .timestamp(alert.getTimestamp())
                .build();
    }

    public static WebhookEvent buildManualDecisionAlert(PaymentAlertEvent alert) {
        return WebhookEvent.builder()
                .paymentId(alert.getPaymentId())
                .payerId(alert.getPayerId())
                .payeeId(alert.getPayeeId())
                .amount(alert.getAmount())
                .webhookType(WebhookType.MANUAL_DECISION_ALERT)
                .status(alert.getAlertType().name())
                .reason(alert.getReason())
                .timestamp(alert.getTimestamp())
                .build();
    }

    public static WebhookEvent buildClientNotificationApproved(
            UUID paymentId, UUID payerId, UUID payeeId, BigDecimal amount, String reason) {
        return WebhookEvent.builder()
                .paymentId(paymentId)
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .webhookType(WebhookType.CLIENT_NOTIFICATION_APPROVED)
                .status("APPROVED")
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebhookEvent buildClientNotificationRejected(
            UUID paymentId, UUID payerId, UUID payeeId, BigDecimal amount, String reason) {
        return WebhookEvent.builder()
                .paymentId(paymentId)
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .webhookType(WebhookType.CLIENT_NOTIFICATION_REJECTED)
                .status("REJECTED")
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebhookEvent buildClientNotificationManualApproved(
            ManualReviewDecision decision, UUID payerId, UUID payeeId, BigDecimal amount) {
        return WebhookEvent.builder()
                .paymentId(decision.getPaymentId())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .webhookType(WebhookType.CLIENT_NOTIFICATION_MANUAL_APPROVED)
                .status(decision.getDecision())
                .reason(decision.getReason())
                .reviewerId(decision.getReviewerId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebhookEvent buildClientNotificationManualRejected(
            ManualReviewDecision decision, UUID payerId, UUID payeeId, BigDecimal amount) {
        return WebhookEvent.builder()
                .paymentId(decision.getPaymentId())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .webhookType(WebhookType.CLIENT_NOTIFICATION_MANUAL_REJECTED)
                .status(decision.getDecision())
                .reason(decision.getReason())
                .reviewerId(decision.getReviewerId())
                .timestamp(LocalDateTime.now())
                .build();
    }
}

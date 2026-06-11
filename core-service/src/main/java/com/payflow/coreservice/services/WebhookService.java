package com.payflow.coreservice.services;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.dto.webhook.WebhookEvent;
import com.payflow.coreservice.builder.WebhookEventBuilder;
import com.payflow.coreservice.strategy.webhook.WebhookContext;
import com.payflow.coreservice.strategy.webhook.WebhookStrategy;
import com.payflow.coreservice.strategy.webhook.factory.WebhookStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookStrategyFactory webhookStrategyFactory;
    private final WebhookContext webhookContext;

    public void sendFraudTeamAlert(PaymentAlertEvent alert){
        WebhookEvent event = WebhookEventBuilder.buildFraudTeamAlert(alert);
        sendWebhook(event);
    }

    public void sendManualDecisionAlert(PaymentAlertEvent alert){
        WebhookEvent event = WebhookEventBuilder.buildManualDecisionAlert(alert);
        sendWebhook(event);
    }

    public void sendClientNotificationApproved(UUID paymentID, UUID payerId, UUID payeeId,
                                               BigDecimal amount, String reason){
        WebhookEvent event = WebhookEventBuilder.buildClientNotificationApproved(
                paymentID, payerId, payeeId, amount, reason);
        sendWebhook(event);
    }

    public void sendClientNotificationRejected(UUID paymentId, UUID payerId, UUID payeeId,
                                               BigDecimal amount, String reason) {
        WebhookEvent event = WebhookEventBuilder.buildClientNotificationRejected(
                paymentId, payerId, payeeId, amount, reason);
        sendWebhook(event);
    }

    public void sendClientNotificationManualApproved(ManualReviewDecision decision, UUID payerId,
                                                     UUID payeeId, BigDecimal amount) {
        WebhookEvent event = WebhookEventBuilder.buildClientNotificationManualApproved(
                decision, payerId, payeeId, amount);
        sendWebhook(event);
    }

    public void sendClientNotificationManualRejected(ManualReviewDecision decision, UUID payerId,
                                                     UUID payeeId, BigDecimal amount) {
        WebhookEvent event = WebhookEventBuilder.buildClientNotificationManualRejected(
                decision, payerId, payeeId, amount);
        sendWebhook(event);
    }

    private void sendWebhook(WebhookEvent event){
        try {
            WebhookStrategy strategy = webhookStrategyFactory.getStrategy(event);
            webhookContext.setStrategy(strategy);
            webhookContext.executeWebhook(event);
        }catch (Exception e){
            log.error("Falha ao enviar webhook: Event={}, Error={}", event.getWebhookType());
            throw new RuntimeException("Falha ao enviar webhook", e);
        }
    }
}

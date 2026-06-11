package com.payflow.coreservice.strategy.webhook;

import com.payflow.commons.dto.webhook.WebhookEvent;

public interface WebhookStrategy {
    void sendWebhook(WebhookEvent event);
    String getWebhookUrl();
    boolean supports(WebhookEvent event);
}

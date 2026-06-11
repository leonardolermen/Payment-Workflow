package com.payflow.coreservice.strategy.webhook.factory;

import com.payflow.commons.dto.webhook.WebhookEvent;
import com.payflow.commons.enums.webhook.WebhookType;
import com.payflow.coreservice.strategy.webhook.WebhookStrategy;
import com.payflow.coreservice.strategy.webhook.impl.ClientNotificationWebhookStrategy;
import com.payflow.coreservice.strategy.webhook.impl.FraudTeamWebhookStrategy;
import com.payflow.coreservice.strategy.webhook.impl.ManualDecisionTeamWebhookStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class WebhookStrategyFactory {

    private final Map<WebhookType, WebhookStrategy> strategyMap;

    private WebhookStrategyFactory(
            FraudTeamWebhookStrategy fraudTeamWebhookStrategy,
            ManualDecisionTeamWebhookStrategy manualDecisionTeamWebhookStrategy,
            ClientNotificationWebhookStrategy clientNotificationWebhookStrategy){

        this.strategyMap = new EnumMap<>(WebhookType.class);

        strategyMap.put(WebhookType.FRAUD_TEAM_ALERT, fraudTeamWebhookStrategy);
        strategyMap.put(WebhookType.MANUAL_DECISION_ALERT, manualDecisionTeamWebhookStrategy);

        strategyMap.put(WebhookType.CLIENT_NOTIFICATION_APPROVED, clientNotificationWebhookStrategy);
        strategyMap.put(WebhookType.CLIENT_NOTIFICATION_REJECTED, clientNotificationWebhookStrategy);
        strategyMap.put(WebhookType.CLIENT_NOTIFICATION_MANUAL_APPROVED, clientNotificationWebhookStrategy);
        strategyMap.put(WebhookType.CLIENT_NOTIFICATION_MANUAL_REJECTED, clientNotificationWebhookStrategy);
    }

    public WebhookStrategy getStrategy(WebhookType webhookType){
        WebhookStrategy strategy = strategyMap.get(webhookType);
        if (strategy == null){
            log.error("Nenhum strategy de webhook foi configurado para o tipo: {}", webhookType);
            throw new IllegalArgumentException("Nenhum strategy de webhook foi configurado para o tipo: " + webhookType);
        }
        return strategy;
    }

    public WebhookStrategy getStrategy(WebhookEvent event){
        return getStrategy(event.getWebhookType());
    }
}

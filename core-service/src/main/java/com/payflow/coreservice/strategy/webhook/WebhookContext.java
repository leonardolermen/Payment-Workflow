package com.payflow.coreservice.strategy.webhook;

import com.payflow.commons.dto.webhook.WebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookContext {

    private WebhookStrategy strategy;

    public void setStrategy(WebhookStrategy strategy){
        this.strategy = strategy;
    }

    public void executeWebhook(WebhookEvent event){
        if(strategy == null){
            log.error("Nenhum strategy de webhook foi configurado para o evento", event.getWebhookType());
            throw new IllegalStateException("Strategy de webhook não configurada");
        }

        if (!strategy.supports(event)){
            log.error("Strategy não suporta esse tipo de evento: {}", event.getWebhookType());
            throw new IllegalArgumentException("Strategy não suporta esse tipo de evento");
        }

        log.info("Executando webhook com o stragegy para o tipo de evento: {}", event.getWebhookType());
        strategy.sendWebhook(event);
    }
}

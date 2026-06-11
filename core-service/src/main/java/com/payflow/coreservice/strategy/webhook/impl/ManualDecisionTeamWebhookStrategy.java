package com.payflow.coreservice.strategy.webhook.impl;

import com.payflow.commons.dto.webhook.WebhookEvent;
import com.payflow.commons.enums.webhook.WebhookType;
import com.payflow.coreservice.security.HmacSignatureService;
import com.payflow.coreservice.strategy.webhook.WebhookStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ManualDecisionTeamWebhookStrategy implements WebhookStrategy {

    private final HmacSignatureService hmacSignatureService;
    private final RestTemplate restTemplate;

    @Value("${WEBHOOK_URL_MANUAL_DECISION_TEAM}")
    private String webhookUrl;

    public ManualDecisionTeamWebhookStrategy(HmacSignatureService hmacSignatureService){
        this.hmacSignatureService = hmacSignatureService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendWebhook(WebhookEvent event) {
        try{
            String signature = hmacSignatureService.generateSignature(event);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Signature", signature);
            headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("X-Webhook-Event", event.getWebhookType().name());

            HttpEntity<WebhookEvent> request = new HttpEntity<>(event, headers);

            log.info("Enviando o webhook para o Time de Decisão Manual: URL={}, PaymentId={}, Type={}",
                    webhookUrl, event.getPaymentId(), event.getWebhookType());

            var response = restTemplate.postForEntity(webhookUrl, request, String.class);

            if(response.getStatusCode().is2xxSuccessful()){
                log.info("Webhook enviado com sucesso ao Time de Decisão Manual: PaymentId={}", event.getWebhookType());
            }else {
                log.error("Falha ao enviar o webhook para o Time de Decisão Manual: Status={}, PaymentId={}",
                        response.getStatusCode(), event.getPaymentId());
            }
        }catch (Exception e){
            log.error("Erro ao enviar o webhook para o Time de Decisão Manual: PaymentId={}, Error={}",
                    event.getPaymentId(), e.getMessage());
            throw new RuntimeException("Falha ao enviar o webhook para o Time de Decisão Manual", e);
        }
    }

    @Override
    public String getWebhookUrl(){
        return webhookUrl;
    }

    @Override
    public boolean supports(WebhookEvent event){
        return event.getWebhookType() == WebhookType.MANUAL_DECISION_ALERT;
    }
}

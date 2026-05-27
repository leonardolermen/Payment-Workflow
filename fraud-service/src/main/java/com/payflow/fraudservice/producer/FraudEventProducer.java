package com.payflow.fraudservice.producer;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendManualDecision(ManualReviewDecision decision){
        log.info("Decisão a ser enviada");
        kafkaTemplate.send("payflow.manual.decision" , decision);
        log.info("Decisão manual enviada: PaymentId={}, Decision={}, Reviewer={}",
                decision.getPaymentId(), decision.getDecision(), decision.getReviewerId());
    }
}

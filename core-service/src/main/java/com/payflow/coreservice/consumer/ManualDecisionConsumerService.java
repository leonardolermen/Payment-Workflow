package com.payflow.coreservice.consumer;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.coreservice.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualDecisionConsumerService {

    private final PaymentService paymentService;

    @KafkaListener
            (topics = "payflow.manual.decision",
            groupId = "manual-decision-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleManualDecision(ManualReviewDecision decision){
        log.info("Decisão manual recebida: PaymentId={}, Decision={}, Reviewer={}" ,
                decision.getPaymentId(), decision.getDecision(), decision.getReviewerId(), decision.getReason());

        try{
            if ("APPROVED".equalsIgnoreCase(decision.getDecision())){
                paymentService.approveManualPayment(decision.getPaymentId(), decision.getReason());
            } else if ("REJECTED".equalsIgnoreCase(decision.getDecision())) {
                paymentService.rejectManualPayment(decision.getPaymentId(), decision.getReason());
            }else {
                log.error("Decisão inválida: {}", decision.getDecision());
            }
        } catch (Exception e) {
            log.error("Erro ao processar decisão manual: PaymentId={}, Error={}",
                    decision.getPaymentId(), e.getMessage());
            throw e;
        }
    }
}

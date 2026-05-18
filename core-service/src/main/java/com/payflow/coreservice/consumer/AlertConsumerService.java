package com.payflow.coreservice.consumer;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.alert.PaymentAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AlertConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(AlertConsumerService.class);

    @KafkaListener(
            topics = "payflow.payment.alerts",
            groupId = "alert-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlerPaymentAlert(PaymentAlertEvent alert){
        logger.info("Alerta recebido: {} | Pagamento: {}",
                alert.getAlertType(), alert.getPaymentId());
        try{
            switch (alert.getAlertType()) {
                case "PENDING_REVIEW":
                    sendEmailToAnalysisTeam(alert);
                    break;
                case "MANUAL_ANALYSIS":
                    notifyManualAnalysisSystem(alert);
                    break;
                case "SUSPICIOUS":
                    notifySecurityTeam(alert);
                    break;
            }
        } catch (Exception e) {
            logger.error("Erro ao processar alerta: {} | Pagamento: {} | Erro: {}",
                    alert.getAlertType(), alert.getPaymentId(), e.getMessage(), e);
            throw e;
        }
    }

    private void sendEmailToAnalysisTeam(PaymentAlertEvent alert){
        if(alert.getAlertType().equals("PENDING_REVIEW")){
            logger.info("Enviando email para equipe de análise: {} | Pagamento: {}",
                    alert.getAlertType(), alert.getPaymentId());
        }
    }

    private void notifyManualAnalysisSystem(PaymentAlertEvent alert){
        if (alert.getAlertType().equals("MANUAL_ANALYSIS")){
            logger.info("Enviando email para a equipe de análise manual: {} | Payment: {}",
                    alert.getAlertType(), alert.getPaymentId());
        }
    }

    private void notifySecurityTeam(PaymentAlertEvent alert){
        if(alert.getAlertType().equals("SUSPICIOUS")){
            logger.info("Enviando email para equipe de segurança: {} | Pagamento: {}",
                    alert.getAlertType(), alert.getPaymentId());
        }
    }

    @KafkaListener(
            topics = "payflow.review.completed",
            groupId = "review-notification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlerReviewCompleted(ManualReviewDecision decision){
        logger.info("Decisão processada e notificada: {} | Pagamento: {}",
                decision.getDecision(), decision.getPaymentId());
        try{
            notifyExternalSystem(decision);
        } catch (Exception e) {
            logger.error("Erro ao notificar sistema externo: {} | Pagamento: {} | Erro: {}",
                    decision.getDecision(), decision.getPaymentId(), e.getMessage(), e);
            throw e;
        }

    }

    private void notifyExternalSystem(ManualReviewDecision decision){
        logger.info("🔄 Notificando sistema externo da decisão: {} | Pagamento: {} | Analista: {}",
                decision.getDecision(), decision.getPaymentId(), decision.getReviewerId());

        try{
            logger.info("✅ Sistema externo notificado com sucesso");
        } catch (Exception e) {
            logger.error("❌ Falha ao notificar sistema externo: {}", e.getMessage());
        }
    }
}

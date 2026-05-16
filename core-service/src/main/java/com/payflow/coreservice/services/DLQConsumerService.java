package com.payflow.coreservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DLQConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(DLQConsumerService.class);

    @KafkaListener(
            topics = "payflow.payment.alerts.dlq" ,
            groupId = "dlq-group",
            containerFactory = "kafkaListenerContainerFactory")
    private void handlerPaymentAlertsDLQ(String message){
        logger.error("Mensagem enviada para DLQ (payflow.payment.alerts): {}", message);
    }

    @KafkaListener(
            topics = "payflow.review.completed.dlq",
            groupId = "dlq-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlerReviewCompletedDLQ(String message){
        logger.error("Mensagem enviada para DLQ (payflow.review.completed): {}", message);
    }
}

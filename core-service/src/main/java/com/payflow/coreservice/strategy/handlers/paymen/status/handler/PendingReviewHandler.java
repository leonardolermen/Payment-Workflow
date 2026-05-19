package com.payflow.coreservice.strategy.handlers.paymen.status.handler;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.builder.PaymentAlertEventBuilder;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingReviewHandler implements PaymentStatusHandler {

    private static final Logger logger = LoggerFactory.getLogger(PendingReviewHandler.class);

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentAlertEvent> kafkaTemplate;

    private static final String ALERT_TOPIC = "payflow.payment.alerts";

    @Override
    @Transactional
    public void handle(Payment payment, FraudAnalysisResponse response){
        payment.setStatus(Enum_Payment.PENDING);
        paymentRepository.save(payment);

        PaymentAlertEvent alertEvent = PaymentAlertEventBuilder.fromAlertEvento(payment, response);

        kafkaTemplate.send(ALERT_TOPIC, payment.getUuid().toString(), alertEvent);

        logger.info("🚨 Pagamento em análise manual: {} | Alerta enviado via Kafka", payment.getUuid());

    }
}

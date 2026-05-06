package com.payflow.coreservice.strategy.handlers;

import com.payflow.commons.dto.alert.PaymentAlertEvent;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SuspiciousHandler implements PaymentStatusHandler {

    private static final Logger logger = LoggerFactory.getLogger(SuspiciousHandler.class);

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentAlertEvent> kafkaTemplate;

    private static final String ALERT_TOPIC = "payflow.payment.alerts";

    @Override
    @Transactional
    public void handle(Payment payment, FraudAnalysisResponse response) {
        payment.setStatus(Enum_Payment.PENDING);
        paymentRepository.save(payment);

        PaymentAlertEvent alertEvent = PaymentAlertEvent.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .alertType("SUSPICIOUS")
                .reason("Atividade suspeita detectada - requer investigação")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(ALERT_TOPIC, payment.getUuid().toString(), alertEvent);

        logger.info("🚨 Atividade suspeita detectada: {} | Alerta crítico enviado via Kafka", payment.getUuid());
    }
}

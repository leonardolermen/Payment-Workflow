package com.payflow.fraudservice.service;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.fraudservice.Repository.FraudLogRepository;
import com.payflow.fraudservice.builder.DecisionEventBuilder;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import com.payflow.fraudservice.producer.FraudEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualReviewService {

    private final FraudLogRepository fraudLogRepository;
    private final FraudEventProducer fraudEventProducer;

    @Transactional
    public void processPaymentDecision(UUID paymentId, String decision,
                                       String reviewerId, String reason){
        log.info("Processando decisão manual: PaymentId={}, Decision={}, Reviewer={}",
                paymentId, decision, reviewerId);

        FraudAnalysisLog log = fraudLogRepository.findByPaymentId(paymentId)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Log de fraud não encontrado"));

        Status_Fraud newStatus = "APPROVED".equalsIgnoreCase(decision)
                ? Status_Fraud.APPROVED
                : Status_Fraud.REJECTED;

        log.setStatus(newStatus);
        log.setReason(reason);
        log.setEvaluatedAt(LocalDateTime.now());
        fraudLogRepository.save(log);

        ManualReviewDecision decisionEvent = DecisionEventBuilder.build(
                paymentId, decision, reviewerId, reason);

        fraudEventProducer.sendManualDecision(decisionEvent);
    }
}

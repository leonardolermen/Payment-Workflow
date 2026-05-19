package com.payflow.coreservice.services;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.coreservice.builder.DecisionBuilder;
import com.payflow.coreservice.builder.PaymentDetailsBuilder;
import com.payflow.coreservice.builder.StatusHistoryBuilder;
import com.payflow.coreservice.dto.factory.PaymentDetailsRequest;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.dto.factory.PaymentDetailsRequest;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.StatusHistory;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.StatusHistoryRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import com.payflow.coreservice.strategy.factory.PaymentStatusHandlerFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualReviewService {

    private final PaymentRepository paymentRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PaymentStatusHandlerFactory handlerFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final List<ManualReviewDecision> decisionHistory = new ArrayList<>();

    public List<PaymentDetailsRequest> getPendingPayments(){
        return paymentRepository.findByStatus(Enum_Payment.PENDING)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    public PaymentDetailsRequest getPaymentDetails(UUID paymentId){
        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));
        return toDetailsDTO(payment);
    }

    @Transactional
    public void processDecision(ManualReviewDecision decision, UUID paymentId){

        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));

        log.info("Processando decisão manual: PaymentId={}, Decision={}, Reviewer={}",
                paymentId, decision.getDecision(), decision.getReviewerId());

        StatusHistory history = StatusHistoryBuilder.fromManualReview(
                paymentId, payment.getStatus(), decision);
        statusHistoryRepository.save(history);

        FraudAnalysisResponse response = DecisionBuilder.fromDecision(decision);

        PaymentStatusHandler handler = handlerFactory.getHandler(response.getStatus());
        handler.handle(payment, response);

    }

    public List<StatusHistory> getHistoryBySource(String source){
        return statusHistoryRepository.findBySource(source);
    }

    public List<StatusHistory> getHistoryByPaymentId(UUID paymentId){
        return statusHistoryRepository.findByOwnerId(paymentId);
    }

    private PaymentDetailsRequest toDetailsDTO(Payment payment){
        return PaymentDetailsBuilder.fromDetails(payment);
    }

}

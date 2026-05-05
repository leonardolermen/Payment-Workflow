package com.payflow.coreservice.services;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.coreservice.dto.factory.PaymentDetailsDTO;
import com.payflow.coreservice.enums.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.repository.PaymentRepository;
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
    private final PaymentStatusHandlerFactory handlerFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final List<ManualReviewDecision> decisionHistory = new ArrayList<>();

    public List<PaymentDetailsDTO> getPendingPayments(){
        return paymentRepository.findByStatus(Enum_Payment.PENDING)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    public PaymentDetailsDTO getPaymentDetails(UUID paymentId){
        Payment payment = paymentRepository.findByUuid(paymentId)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));
        return toDetailsDTO(payment);
    }

    @Transactional
    public void processDecision(ManualReviewDecision decision){
        Payment payment = paymentRepository.findByUuid(decision.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));

        log.info("Processando decisão manual: PaymentId={}, Decision={}, Reviewer={}",
                decision.getPaymentId(), decision.getDecision(), decision.getReviewerId());


        FraudAnalysisResponse mockResponse = FraudAnalysisResponse.builder()
                .paymentId(decision.getPaymentId())
                .status(decision.getDecision().equals("APPROVED") ?
                    Status_Fraud.APPROVED : Status_Fraud.REJECTED)
                .reason(decision.getReason())
                .build();

        PaymentStatusHandler handler = handlerFactory.getHandler(mockResponse.getStatus());
        handler.handle(payment, mockResponse);

       // decisionHistory.add(decision);

        notifyDecisionProcessed(decision);
    }

    /*
    private List<ManualReviewDecision> getDecisionHistory(){
        return new ArrayList<>(decisionHistory);
    }

     */

    private PaymentDetailsDTO toDetailsDTO(Payment payment){
        return PaymentDetailsDTO.builder()
                .paymentId(payment.getUuid())
                .payerId(payment.getPayerId())
                .payeeId(payment.getPayeeId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private void notifyDecisionProcessed(ManualReviewDecision decision){

        kafkaTemplate.send("payflow.review.completed", decision);

        log.info("Decisão processada e notificada: {}", decision.getPaymentId());
    }
}

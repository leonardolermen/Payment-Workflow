package com.payflow.fraudservice.service;

import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import com.payflow.fraudservice.Enums.user.User_Status;
import com.payflow.fraudservice.Repository.FraudLog_Repository;
import com.payflow.fraudservice.client.CoreServiceClient;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisRequestDTO;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisResponseDTO;
import com.payflow.fraudservice.dto.payment.PaymentResponseDTO;
import com.payflow.fraudservice.dto.user.UserRecordDTO;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class FraudAnalysisService {

    private final CoreServiceClient coreServiceClient;

    private final FraudLog_Repository fraudLogRepository;


    public FraudAnalysisService(CoreServiceClient coreServiceClient, FraudLog_Repository fraudLogRepository) {
        this.coreServiceClient = coreServiceClient;
        this.fraudLogRepository = fraudLogRepository;
    }

    public FraudAnalysisResponseDTO analyzePayment(FraudAnalysisRequestDTO request){

        PaymentResponseDTO payment = coreServiceClient.getPaymentById(request.paymentId());

        UserRecordDTO payer = coreServiceClient.getUserById(request.payerId());
        UserRecordDTO payee = coreServiceClient.getUserById(request.payeeId());

        double score = calculateRiskScore(payment, payer, payee);
        Status_Fraud status = determineStatus(score);
        String reason = determineReason(score, payment);

        FraudAnalysisLog log = new FraudAnalysisLog();
        log.setPaymentId(request.paymentId());
        log.setScore(score);
        log.setStatus(status);
        log.setReason(reason);
        fraudLogRepository.save(log);

        return new FraudAnalysisResponseDTO(status, score, reason);
    }

    private double calculateRiskScore(PaymentResponseDTO payment, UserRecordDTO payer, UserRecordDTO payee) {
        double score = 0;

        if(payment.amount().compareTo(new BigDecimal("15000"))> 0){
            score += 50;
        }

        if(payer.balance().compareTo(payment.amount())< 0){
            score += 30;
        }

        if(payer.status() != User_Status.ACTIVE){
            score += 40;
        }

        if(payee.status() != User_Status.ACTIVE){
            score += 30;
        }

        if(payee.createdAt().isAfter(LocalDateTime.now().minusDays(30))){
            score += 30;
        }

        int recentPayeeTransactions = coreServiceClient.getRecentTransationCount(payee.id(), "24");
        if(recentPayeeTransactions > 5){
            score += 30;
        }

        int recentPayerTransactions = coreServiceClient.getRecentTransationCount(payer.id(), "1");
        if(recentPayerTransactions > 5){
            score += 30;
        }
        return Math.min(score, 100);
    }

    private Status_Fraud determineStatus(double score){
        if(score > 70) return Status_Fraud.REJECTED;
        if(score >= 30) return Status_Fraud.MANUAL_ANALYSIS;
        return Status_Fraud.APPROVED;
    }

    private String determineReason(double score, PaymentResponseDTO payment){
        if(score > 70) return "Alto risco detectado";
        if(score >= 30) return "Análise manual necessária";
        return "Transação aprovada";
    }
}


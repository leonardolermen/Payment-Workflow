package com.payflow.fraudservice.service;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import com.payflow.commons.enums.user.User_Status;
import com.payflow.fraudservice.Repository.FraudLogRepository;
import com.payflow.fraudservice.client.CoreServiceClient;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisRequest;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisResponse;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class FraudAnalysisService {

    private final CoreServiceClient coreServiceClient;

    private final FraudLogRepository fraudLogRepository;


    public FraudAnalysisService(CoreServiceClient coreServiceClient, FraudLogRepository fraudLogRepository) {
        this.coreServiceClient = coreServiceClient;
        this.fraudLogRepository = fraudLogRepository;
    }

    public FraudAnalysisResponse analyzePayment(FraudAnalysisRequest request){

        PaymentResponse payment = coreServiceClient.getPaymentById(request.paymentId());

        UserResponse payer = coreServiceClient.getUserById(request.payerId());
        UserResponse payee = coreServiceClient.getUserById(request.payeeId());

        double score = calculateRiskScore(payment, payer, payee);
        Status_Fraud status = determineStatus(score);
        String reason = determineReason(score, payment);

        FraudAnalysisLog log = new FraudAnalysisLog();
        log.setPaymentId(request.paymentId());
        log.setScore(score);
        log.setStatus(status);
        log.setReason(reason);
        log.setEvaluatedAt(LocalDateTime.now());
        fraudLogRepository.save(log);

        return FraudAnalysisResponse.builder()
                .status(status)
                .score(score)
                .reason(reason)
                .build();
    }

    private double calculateRiskScore(PaymentResponse payment, UserResponse payer, UserResponse payee) {
        double score = 0;

        if(payment.getAmount().compareTo(new BigDecimal("40000"))> 0){
            score += 30;
        }

        if(payer.getBalance().compareTo(payment.getAmount())< 0){
            score += 30;
        }

        if(payer.getStatus() != User_Status.ACTIVE){
            score += 40;
        }

        if(payee.getStatus() != User_Status.ACTIVE){
            score += 30;
        }

        if(payee.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)) && payment.getAmount().compareTo(new BigDecimal("35000"))> 0){
            score += 70;
        }

//        int recentPayeeTransactions = coreServiceClient.getRecentTransationCount(payee.getId(), "24");
//        if(recentPayeeTransactions > 5){
//            score += 30;
//        }
//
//        int recentPayerTransactions = coreServiceClient.getRecentTransationCount(payer.getId(), "1");
//        if(recentPayerTransactions > 5){
//            score += 30;
//        }
        return Math.min(score, 100);
    }

    private Status_Fraud determineStatus(double score){
        if(score >= 70) return Status_Fraud.REJECTED;
        if(score >= 30) return Status_Fraud.MANUAL_ANALYSIS;
        return Status_Fraud.APPROVED;
    }

    private String determineReason(double score, PaymentResponse payment){
        if(score > 70) return "Alto risco detectado";
        if(score >= 30) return "Análise manual necessária";
        return "Transação aprovada";
    }
}


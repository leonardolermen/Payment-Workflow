package com.payflow.fraudservice.service;

import com.payflow.fraudservice.Repository.FraudLog_Repository;
import com.payflow.fraudservice.client.CoreServiceClient;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisRequestDTO;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisResponseDTO;
import com.payflow.fraudservice.dto.payment.PaymentResponseDTO;
import com.payflow.fraudservice.dto.user.UserRecordDTO;

import java.math.BigDecimal;

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
        return null; //Mudar
    }

    private double calculateRiskScore(PaymentResponseDTO payment, UserRecordDTO payer, UserRecordDTO payee) {
        double score = 0;

        double count_transactions = 0;

        if(payment.amount().compareTo(new BigDecimal("15000"))> 0){
            score += 50;
        }
        return score; //Mudar
    }
}


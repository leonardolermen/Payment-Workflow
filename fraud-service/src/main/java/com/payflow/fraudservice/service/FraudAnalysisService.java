package com.payflow.fraudservice.service;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import com.payflow.fraudservice.Repository.FraudLogRepository;
import com.payflow.fraudservice.client.CoreServiceClient;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisRequest;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisResponse;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import com.payflow.fraudservice.service.cache.TransactionHistoryCacheService;
import com.payflow.fraudservice.service.rule.RiskResult;
import com.payflow.fraudservice.service.rule.RiskRuleEngine;
import com.payflow.fraudservice.service.rule.TransactionHistory;
import com.traceflow.sdk.Tracer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class FraudAnalysisService {

    private final CoreServiceClient coreServiceClient;
    private final FraudLogRepository fraudLogRepository;
    private final RiskRuleEngine riskRuleEngine;
    private final TransactionHistoryCacheService historyCacheService;

    public FraudAnalysisService(CoreServiceClient coreServiceClient,
                                FraudLogRepository fraudLogRepository,
                                RiskRuleEngine riskRuleEngine,
                                TransactionHistoryCacheService historyCacheService) {
        this.coreServiceClient = coreServiceClient;
        this.fraudLogRepository = fraudLogRepository;
        this.riskRuleEngine = riskRuleEngine;
        this.historyCacheService = historyCacheService;
    }

    public FraudAnalysisResponse analyzePayment(FraudAnalysisRequest request){

        PaymentResponse payment = coreServiceClient.getPaymentById(request.getPaymentId());

        UserResponse payer = coreServiceClient.getUserById(request.getPayerId());
        UserResponse payee = coreServiceClient.getUserById(request.getPayeeId());

        TransactionHistory history = new TransactionHistory(
                request.getPayerId(),
                historyCacheService.getUserTransactions(request.getPayerId())
        );

        RiskResult result = riskRuleEngine.evaluate(payment, payer, payee, history);
        Status_Fraud status = determineStatus(result.score());
        String reason = determineReason(result.score(), result.triggeredRules());

        if (status == Status_Fraud.REJECTED) {
            Tracer.warn("fraud.rejected", Map.of(
                    "paymentId",     request.getPaymentId().toString(),
                    "payerId",       request.getPayerId().toString(),
                    "score",         String.valueOf(result.score()),
                    "triggeredRules", String.join(", ", result.triggeredRules())
            ));
        } else if (status == Status_Fraud.MANUAL_ANALYSIS) {
            Tracer.warn("fraud.manual_analysis", Map.of(
                    "paymentId",     request.getPaymentId().toString(),
                    "payerId",       request.getPayerId().toString(),
                    "score",         String.valueOf(result.score()),
                    "triggeredRules", String.join(", ", result.triggeredRules())
            ));
        } else {
            Tracer.log("fraud.approved", Map.of(
                    "paymentId", request.getPaymentId().toString(),
                    "payerId",   request.getPayerId().toString(),
                    "score",     String.valueOf(result.score())
            ));
        }

        FraudAnalysisLog log = new FraudAnalysisLog();
        log.setPaymentId(request.getPaymentId());
        log.setScore((double) result.score());
        log.setStatus(status);
        log.setReason(reason);
        log.setEvaluatedAt(LocalDateTime.now());
        fraudLogRepository.save(log);

        historyCacheService.addTransaction(request.getPayerId(), request.getPaymentId(), request.getPayeeId(), payment.getAmount());

        return FraudAnalysisResponse.builder()
                .status(status)
                .score((double) result.score())
                .reason(reason)
                .build();
    }

    private Status_Fraud determineStatus(double score) {
        if (score >= 70) return Status_Fraud.REJECTED;
        if (score >= 30) return Status_Fraud.MANUAL_ANALYSIS;
        return Status_Fraud.APPROVED;
    }

    private String determineReason(double score, java.util.List<String> triggeredRules) {
        if (score >= 70) {
            return "Rejeitado: " + formatRules(triggeredRules);
        }
        if (score >= 30) {
            return "Análise manual: " + formatRules(triggeredRules);
        }
        return "Transação aprovada";
    }

    private String formatRules(java.util.List<String> rules) {
        if (rules.isEmpty()) return "sem regras disparadas";
        StringJoiner joiner = new StringJoiner(", ");
        rules.forEach(joiner::add);
        return joiner.toString();
    }
}


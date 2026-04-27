package com.payflow.fraudservice.controller;

import com.payflow.fraudservice.Repository.FraudLog_Repository;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisRequestDTO;
import com.payflow.fraudservice.dto.fraud.FraudAnalysisResponseDTO;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import com.payflow.fraudservice.service.FraudAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/fraud")
public class FraudController {

    @Autowired
    private FraudAnalysisService fraudAnalysisService;
    @Autowired
    private FraudLog_Repository fraudLog_Repository;

    @PostMapping("/analyze")
    public ResponseEntity<FraudAnalysisResponseDTO> analyzePayment(
            @RequestBody FraudAnalysisRequestDTO request){

        log.info(
                "fraud.analyze.request traceId={} paymentId={} payerId={} payeeId={}",
                MDC.get("traceId"),
                request.paymentId(),
                request.payerId(),
                request.payeeId()
        );
        FraudAnalysisResponseDTO response = fraudAnalysisService.analyzePayment(request);
        log.info(
                "fraud.analyze.response traceId={} paymentId={} status={} score={} ",
                MDC.get("traceId"),
                request.paymentId(),
                response.status(),
                response.score()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analysis/{paymentId}")
    public ResponseEntity<List<FraudAnalysisLog>> getAnalysisByPaymentId(
            @PathVariable UUID paymentId){

        log.info("fraud.analysis.get traceId={} paymentId={}", MDC.get("traceId"), paymentId);
        List<FraudAnalysisLog> analyses = fraudLog_Repository.findByPaymentId(paymentId);
        return ResponseEntity.ok(analyses);
    }

}

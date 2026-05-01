package com.payflow.coreservice.client;

import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "anti-fraud-service", url = "${anti-fraud-service.url}")
public interface AntiFraudClient {

    @PostMapping("/fraud/analyze")
    public ResponseEntity<FraudAnalysisResponse> analyzeTransaction(@RequestBody FraudAnalysisRequest transactionRequest);


}

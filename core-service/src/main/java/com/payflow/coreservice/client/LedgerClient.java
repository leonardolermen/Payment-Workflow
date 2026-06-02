package com.payflow.coreservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "ledger-service", url = "http://localhost:8083")
public interface LedgerClient {
    @PostMapping("/ledger/record")
    Map<String, String> recordPayment(@RequestBody Map<String, Object> request);
}

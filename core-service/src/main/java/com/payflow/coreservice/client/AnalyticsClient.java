package com.payflow.coreservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "analytics-service", url = "http://localhost:8085")
public interface AnalyticsClient {
    @PostMapping("/analytics/track")
    Map<String, Object> trackPayment(@RequestBody Map<String, Object> request);
}

package com.payflow.coreservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "notification-service", url = "${notification-service.url:http://localhost:8084}")
public interface NotificationClient {
    @PostMapping("/notifications/send")
    Map<String, String> sendNotification(@RequestBody Map<String, Object> request);
}

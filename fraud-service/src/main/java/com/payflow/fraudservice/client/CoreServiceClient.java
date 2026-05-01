package com.payflow.fraudservice.client;

import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.fraudservice.dto.authenticator.AuthRequestDTO;
import com.payflow.fraudservice.dto.authenticator.AuthResponseDTO;
import com.payflow.fraudservice.dto.user.UserRecordDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@Service
@FeignClient(name = "core-service", url = "${core-service.url}")
public interface CoreServiceClient {
    @GetMapping("/payments/{paymentId}")
    PaymentResponse getPaymentById(@PathVariable UUID paymentId);

    @GetMapping("/users/{userId}")
    UserResponse getUserById(@PathVariable UUID userId);

    @GetMapping("/users/{userId}/recent-transactions")
    Integer getRecentTransationCount(@PathVariable UUID userId, @RequestParam String period);
}

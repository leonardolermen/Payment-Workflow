package com.payflow.fraudservice.client;

import com.payflow.fraudservice.dto.authenticator.AuthRequestDTO;
import com.payflow.fraudservice.dto.authenticator.AuthResponseDTO;
import com.payflow.fraudservice.dto.payment.PaymentResponseDTO;
import com.payflow.fraudservice.dto.user.UserRecordDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "core-service", url = "http://localhost:8081")
public interface CoreServiceClient {
    @GetMapping("/payments/{paymentId}")
    PaymentResponseDTO getPaymentById(@PathVariable UUID paymentId);

    @GetMapping("/users/{userId}")
    UserRecordDTO getUserById(@PathVariable UUID userId);

    @PostMapping("/auth/login")
    AuthResponseDTO authenticate(@RequestBody AuthRequestDTO authRequest);
}

package com.payflow.coreservice.controller;

import com.payflow.commons.dto.payment.PaymentRequest;
import com.payflow.commons.dto.payment.PaymentResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // =========================
    // POST /payments
    // =========================
    @PostMapping
    public PaymentResponse create(@RequestHeader("idempotency-key") String idempotencyKey,
                                  @RequestBody PaymentRequest request) {
        logger.info("Creating payment idempotencyKey={} payload={}",idempotencyKey, request);
        return paymentService.createPayment(idempotencyKey, request);
    }

    // =========================
    // GET /payments/{id}
    // =========================
    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    // =========================
    // GET /users/{userId}/payments
    // =========================
    @GetMapping("/users/{userId}")
    public List<PaymentResponse> getByUser(@PathVariable UUID userId) {
        return paymentService.getByUser(userId);
    }

    // =========================
    // GET /payments/status/{status}
    // =========================
    @GetMapping("/status/{status}")
    public List<PaymentResponse> getByStatus(@PathVariable Enum_Payment status) {
        return paymentService.getByStatus(status);
    }

    // =========================
    // GET /payments
    // =========================
    @GetMapping
    public List<PaymentResponse> getAll() {
        return paymentService.getAll();
    }
}

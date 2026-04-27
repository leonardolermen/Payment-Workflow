package com.payflow.coreservice.controller;

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
    public PaymentResponseDTO create(@RequestBody PaymentRequestDTO dto) {
        logger.info("Creating payment payload={}", dto);
        return paymentService.createPayment(dto);
    }

    // =========================
    // GET /payments/{id}
    // =========================
    @GetMapping("/{id}")
    public PaymentResponseDTO getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    // =========================
    // GET /users/{userId}/payments
    // =========================
    @GetMapping("/users/{userId}")
    public List<PaymentResponseDTO> getByUser(@PathVariable UUID userId) {
        return paymentService.getByUser(userId);
    }
}

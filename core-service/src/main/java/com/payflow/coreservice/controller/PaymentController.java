package com.payflow.coreservice.controller;

import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.services.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // =========================
    // POST /payments
    // =========================
    @PostMapping
    public PaymentResponseDTO create(@RequestBody PaymentRequestDTO dto) {
        return paymentService.createPayment(dto);
    }

    // =========================
    // GET /payments/{id}
    // =========================
    @GetMapping("/{id}")
    public PaymentResponseDTO getById(@PathVariable Long id) {
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

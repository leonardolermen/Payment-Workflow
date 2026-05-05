package com.payflow.coreservice.controller;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.coreservice.services.ManualReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/manual-review")
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualReviewService manualReviewService;

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPayments(){
        return ResponseEntity.ok(manualReviewService.getPendingPayments());
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable UUID paymentId){
        return ResponseEntity.ok(manualReviewService.getPaymentDetails(paymentId));
    }

    @PostMapping("/decision")
    public ResponseEntity<?> makeDecision(@RequestBody ManualReviewDecision decision){
        manualReviewService.processDecision(decision);
        return ResponseEntity.ok().build();
    }
/*
    @GetMapping("/history")
    public ResponseEntity<?> getDecisionHistory(){
        return ResponseEntity.ok(manualReviewService.getDecisionHistory());
    }

 */
}

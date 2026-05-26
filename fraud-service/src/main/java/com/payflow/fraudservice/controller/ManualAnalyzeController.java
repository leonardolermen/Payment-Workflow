package com.payflow.fraudservice.controller;

import com.payflow.fraudservice.service.ManualReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/manual-analyze")
public class ManualAnalyzeController {

    private final ManualReviewService manualReviewService;

    @PutMapping("/payment/{paymentId}")
    public ResponseEntity<String> manualAnalyze(@PathVariable UUID paymentId,
                                                @RequestParam String decision,
                                                @RequestParam String reviewerId,
                                                @RequestParam(required = false) String reason){
        manualReviewService.processPaymentDecision(paymentId, decision, reviewerId, reason);
        return ResponseEntity.ok("Decisão manual processada com sucesso");
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<String> manualAnalyzeUser(@PathVariable UUID userId){
        //TODO: implementar metodo de analise manual atualizando as tabelas de logs e fraud
        //TODO: implementar kafka para notificar o core service
        return ResponseEntity.ok("Manual analyze");
    }

}

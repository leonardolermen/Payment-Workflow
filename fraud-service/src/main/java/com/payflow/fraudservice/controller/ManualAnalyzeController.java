package com.payflow.fraudservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/manual-analyze")
public class ManualAnalyzeController {

    @PutMapping("/payment/{paymentId}")
    public ResponseEntity<String> manualAnalyze(@PathVariable UUID paymentId){
        //TODO: implementar metodo de analise manual atualizando as tabelas de logs e fraud
        //TODO: implementar kafka para notificar o core service
        return ResponseEntity.ok("Manual analyze");
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<String> manualAnalyzeUser(@PathVariable UUID userId){
        //TODO: implementar metodo de analise manual atualizando as tabelas de logs e fraud
        //TODO: implementar kafka para notificar o core service
        return ResponseEntity.ok("Manual analyze");
    }

}

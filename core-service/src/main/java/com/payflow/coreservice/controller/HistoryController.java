package com.payflow.coreservice.controller;

import com.payflow.coreservice.model.StatusHistory;
import com.payflow.coreservice.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final StatusHistoryRepository statusHistoryRepository;

    @GetMapping("/source/{source}")
    public ResponseEntity<List<StatusHistory>> getHistoryBySource(@PathVariable String source){
        List<StatusHistory> history = statusHistoryRepository.findBySource(source);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<StatusHistory>> getHistoryByPaymentId(@PathVariable UUID paymentId){
        List<StatusHistory> history = statusHistoryRepository.findByOwnerId(paymentId);
        return ResponseEntity.ok(history);
    }
}

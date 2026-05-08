package com.payflow.coreservice.controller;

import com.payflow.coreservice.model.StatusHistory;
import com.payflow.coreservice.services.ManualReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/history")
@RequiredArgsConstructor
public class HistoryController {
    private final ManualReviewService manualReviewService;

    @GetMapping("/source/{source}")
    public ResponseEntity<List<StatusHistory>> getHistoryBySource(@PathVariable String source){
        List<StatusHistory> history = manualReviewService.getHistoryBySource(source);
        return ResponseEntity.ok(history);
    }
}

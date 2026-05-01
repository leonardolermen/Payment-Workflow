package com.payflow.coreservice.controller;

import com.payflow.coreservice.model.Transaction;
import com.payflow.coreservice.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class UserPeriodController {
    private final TransactionRepository transactionRepository;

    public UserPeriodController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/users/{userId}/recent-transactions")
    public Integer getRecentTransationCount(@PathVariable UUID userId, @RequestParam String period) {
        LocalDateTime since;

        switch (period) {
            case "1h":
                since = LocalDateTime.now().minusHours(1);
                break;
            case "6h":
                since = LocalDateTime.now().minusHours(6);
                break;
            case "24h":
            case "1d":
                since = LocalDateTime.now().minusDays(1);
                break;
            default:
                since = LocalDateTime.now().minusHours(1);
        }
        List<Transaction> payerTransactions = transactionRepository.findByPayerIdAndExecutedAtAfter(userId, since);
        List<Transaction> payeeTransactions = transactionRepository.findByPayeeIdAndExecutedAtAfter(userId, since);
        return payerTransactions.size() + payeeTransactions.size();
    }
}

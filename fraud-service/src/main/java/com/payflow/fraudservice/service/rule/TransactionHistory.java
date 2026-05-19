package com.payflow.fraudservice.service.rule;

import com.payflow.fraudservice.service.cache.TransactionHistoryCacheService;

import java.util.List;

public record TransactionHistory(
        UUID payerId,
        List<TransactionHistoryCacheService.TransactionRecord> transactions
) {
    public TransactionHistory(UUID payerId, List<TransactionHistoryCacheService.TransactionRecord> transactions) {
        this.payerId = payerId;
        this.transactions = transactions != null ? transactions : List.of();
    }
}

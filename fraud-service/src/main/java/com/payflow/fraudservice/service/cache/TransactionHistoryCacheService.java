package com.payflow.fraudservice.service.cache;

import com.payflow.commons.dto.payment.PaymentResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionHistoryCacheService {

    private final ConcurrentHashMap<UUID, List<TransactionRecord>> userHistory = new ConcurrentHashMap<>();

    public record TransactionRecord(
            UUID paymentId,
            UUID payeeId,
            BigDecimal amount,
            LocalDateTime timestamp
    ) {}

    public void addTransaction(UUID payerId, UUID paymentId, UUID payeeId, BigDecimal amount) {
        userHistory.computeIfAbsent(payerId, k -> new ArrayList<>())
                .add(new TransactionRecord(paymentId, payeeId, amount, LocalDateTime.now()));
    }

    public List<TransactionRecord> getUserTransactions(UUID payerId) {
        return userHistory.getOrDefault(payerId, new ArrayList<>());
    }

    public long countTransactionsInLastHours(UUID payerId, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return getUserTransactions(payerId).stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .count();
    }

    public long countTransactionsInLastMinutes(UUID payerId, int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        return getUserTransactions(payerId).stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .count();
    }

    public long countTransactionsWithSameAmount(UUID payerId, BigDecimal amount, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return getUserTransactions(payerId).stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .filter(t -> t.amount().compareTo(amount) == 0)
                .count();
    }

    public void clearOldTransactions(UUID payerId, int hoursToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursToKeep);
        userHistory.computeIfPresent(payerId, (k, transactions) -> {
            transactions.removeIf(t -> t.timestamp().isBefore(cutoff));
            return transactions.isEmpty() ? null : transactions;
        });
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearAllHistoryDaily() {
        userHistory.clear();
    }

    public void clearAllHistory() {
        userHistory.clear();
    }
}

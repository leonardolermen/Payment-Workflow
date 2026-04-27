package com.payflow.coreservice.repository;

import com.payflow.coreservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository <Transaction, Long >  {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByPayerIdAndExecutedAtAfter(UUID payerId, LocalDateTime since);

    List<Transaction> findByPayeeIdAndExecutedAtAfter(UUID payeeId, LocalDateTime since);
}

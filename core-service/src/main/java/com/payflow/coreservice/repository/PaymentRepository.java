package com.payflow.coreservice.repository;

import com.payflow.coreservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByPayerIdOrPayeeId(UUID payerId, UUID payeeId);

}

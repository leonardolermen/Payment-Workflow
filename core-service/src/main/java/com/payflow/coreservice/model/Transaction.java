package com.payflow.coreservice.model;
import com.payflow.coreservice.Enum_Transaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Enum_Transaction status;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime executedAt;



    @PrePersist
    public void prePersist() {
        if(uuid == null){
            uuid = UUID.randomUUID();
        }

    }
}

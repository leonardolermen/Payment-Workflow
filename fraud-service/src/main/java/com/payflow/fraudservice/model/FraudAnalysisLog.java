package com.payflow.fraudservice.model;
import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table (name = "fraud_analysis_logs")
public class FraudAnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, updatable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status_Fraud status;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null){
            uuid = UUID.randomUUID();
        }
    }
}

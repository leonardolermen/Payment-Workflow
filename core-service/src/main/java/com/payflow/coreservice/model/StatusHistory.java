package com.payflow.coreservice.model;

import com.payflow.commons.enums.payment.Enum_Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Enum_Payment oldStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Enum_Payment newStatus;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "change_reason", nullable = false)
    private String changeReason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "source", nullable = false)
    private String source;
}

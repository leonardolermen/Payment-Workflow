package com.payflow.coreservice.model;

import com.payflow.commons.enums.payment.Enum_Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistory {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Enum_Payment oldStatus;

    @Enumerated(EnumType.STRING)
    private Enum_Payment newStatus;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "source")
    private String source;
}

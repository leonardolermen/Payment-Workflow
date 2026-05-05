package com.payflow.commons.dto.alert;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentAlertEvent {
    private UUID paymentId;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private String alertType;
    private String reason;
    private LocalDateTime timestamp;
}

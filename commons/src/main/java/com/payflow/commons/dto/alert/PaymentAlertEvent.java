package com.payflow.commons.dto.alert;

import com.payflow.commons.enums.alert.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentAlertEvent {
    private UUID paymentId;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private AlertType alertType;
    private String reason;
    private LocalDateTime timestamp;
}

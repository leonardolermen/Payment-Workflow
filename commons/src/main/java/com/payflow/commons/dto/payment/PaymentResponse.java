package com.payflow.commons.dto.payment;

import com.payflow.commons.enums.payment.Enum_Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private Enum_Payment status;
    private LocalDateTime createdAt;
    private String idempotencyKey;

}

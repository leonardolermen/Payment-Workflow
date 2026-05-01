package com.payflow.coreservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.payflow.coreservice.enums.Enum_Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {

    private UUID id;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private Enum_Payment status;
    private LocalDateTime createdAt;

}

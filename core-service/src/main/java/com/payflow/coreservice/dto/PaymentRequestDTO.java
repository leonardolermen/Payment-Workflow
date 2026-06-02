package com.payflow.coreservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentRequestDTO {

    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private String idempotencyKey;

}

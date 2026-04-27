package com.payflow.coreservice.dto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequestDTO {

    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private String idempotencyKey;

}

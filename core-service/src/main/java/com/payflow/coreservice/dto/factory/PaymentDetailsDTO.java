package com.payflow.coreservice.dto.factory;

import com.payflow.commons.enums.payment.Enum_Payment;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class PaymentDetailsDTO {
    private UUID paymentId;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private Enum_Payment status;
    private LocalDateTime createdAt;
}

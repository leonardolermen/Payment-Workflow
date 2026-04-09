package com.payflow.fraudservice.dto.payment;
import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponseDTO(
        UUID id,
        BigDecimal amount,
        UUID payerId,
        UUID payeeId,
        Status_Fraud status
) {
}

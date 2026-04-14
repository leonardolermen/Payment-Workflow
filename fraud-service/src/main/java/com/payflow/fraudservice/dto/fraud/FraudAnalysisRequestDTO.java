package com.payflow.fraudservice.dto.fraud;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudAnalysisRequestDTO(
        UUID paymentId,
        BigDecimal amount,
        UUID payerId,
        UUID payeeId
) {}

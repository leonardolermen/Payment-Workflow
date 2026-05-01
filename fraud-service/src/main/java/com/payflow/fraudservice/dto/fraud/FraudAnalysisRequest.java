package com.payflow.fraudservice.dto.fraud;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudAnalysisRequest(
        UUID paymentId,
        BigDecimal amount,
        UUID payerId,
        UUID payeeId
) {}

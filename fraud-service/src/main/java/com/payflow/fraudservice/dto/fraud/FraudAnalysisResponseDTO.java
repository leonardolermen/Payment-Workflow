package com.payflow.fraudservice.dto.fraud;

import com.payflow.fraudservice.Enums.fraud.Status_Fraud;

public record FraudAnalysisResponseDTO(
        Status_Fraud status,
        Double score,
        String reason

) {
}

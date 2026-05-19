package com.payflow.fraudservice.dto.fraud;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FraudAnalysisRequest {

    private UUID paymentId;
    private BigDecimal amount;
    private UUID payerId;
    private UUID payeeId;

}



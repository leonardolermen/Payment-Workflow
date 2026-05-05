package com.payflow.commons.dto.fraud;

import com.payflow.commons.enums.fraud.Status_Fraud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FraudAnalysisResponse {

    private Status_Fraud status;
    private Double score;
    private String reason;
    private UUID paymentId;

}

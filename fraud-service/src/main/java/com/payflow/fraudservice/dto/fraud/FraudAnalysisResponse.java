package com.payflow.fraudservice.dto.fraud;

import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FraudAnalysisResponse {

    private Status_Fraud status;
    private Double score;
    private String reason;

}

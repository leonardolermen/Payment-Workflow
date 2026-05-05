package com.payflow.commons.dto.alert;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ManualReviewDecision {
    private UUID paymentId;
    private String reviewerId;
    private String decision;
    private String reason;
    private String notes;
}

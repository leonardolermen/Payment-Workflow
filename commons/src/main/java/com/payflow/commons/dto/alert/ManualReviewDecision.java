package com.payflow.commons.dto.alert;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ManualReviewDecision {
    private UUID paymentId;
    private String reviewerId;
    private String decision;
    private String reason;
    private String notes;
}

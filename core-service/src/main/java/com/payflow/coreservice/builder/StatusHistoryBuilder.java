package com.payflow.coreservice.builder;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.model.StatusHistory;

import java.time.LocalDateTime;
import java.util.UUID;

public class StatusHistoryBuilder {
    public static StatusHistory fromManualReview(
            UUID paymentId,
            Enum_Payment oldStatus,
            ManualReviewDecision decision){

        Enum_Payment newStatus = decision.getDecision().equals("APPROVED") ?
            Enum_Payment.APPROVED : Enum_Payment.REJECTED;

        return StatusHistory.builder()
                .id(UUID.randomUUID())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(decision.getReviewerId())
                .changeReason(decision.getReason())
                .timestamp(LocalDateTime.now())
                .source("MANUAL_REVIEW")
                .build();
    }
}

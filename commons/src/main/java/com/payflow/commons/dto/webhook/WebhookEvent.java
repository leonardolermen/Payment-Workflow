package com.payflow.commons.dto.webhook;

import com.payflow.commons.enums.webhook.WebhookType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
    private UUID paymentId;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private WebhookType webhookType;
    private String status;
    private String reason;
    private String reviewerId;
    private LocalDateTime timestamp;
    private Object metadata;
}

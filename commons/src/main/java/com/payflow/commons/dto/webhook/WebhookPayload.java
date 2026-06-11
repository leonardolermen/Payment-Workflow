package com.payflow.commons.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String eventId;
    private WebhookEvent event;
    private String signature;
    private Long timestamp;
}

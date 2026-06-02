package com.payflow.commons.dto.user;

import com.payflow.commons.enums.kyc.Kyc_Result;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserKycResultEvent {

    private UUID userId;

    private Kyc_Result result;

    private Integer score;

    private List<String> triggeredRules;

    private String reason;

    private LocalDateTime analyzedAt;
}

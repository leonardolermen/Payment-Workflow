package com.payflow.coreservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String type;
    private UUID userId;
    private String name;
    private String email;
    private LocalDateTime expiresAt;
}

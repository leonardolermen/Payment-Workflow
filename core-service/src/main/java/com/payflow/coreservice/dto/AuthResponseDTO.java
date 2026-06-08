package com.payflow.coreservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthResponseDTO {
    private String token;
    private String type;
    private UUID userId;
    private String name;
    private String email;
    private LocalDateTime expiresAt;
}

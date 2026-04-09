package com.payflow.fraudservice.dto.authenticator;

import java.util.UUID;

public record AuthResponseDTO(
        String token,
        UUID userId,
        String name,
        String email
) {
}

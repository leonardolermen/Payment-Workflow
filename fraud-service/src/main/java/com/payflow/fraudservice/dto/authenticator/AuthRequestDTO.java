package com.payflow.fraudservice.dto.authenticator;

public record AuthRequestDTO(
        String email,
        String password
) {
}

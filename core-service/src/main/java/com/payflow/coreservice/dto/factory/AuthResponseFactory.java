package com.payflow.coreservice.dto.factory;

import com.payflow.coreservice.dto.AuthResponseDTO;
import com.payflow.coreservice.model.User;

import java.time.LocalDateTime;

public final class AuthResponseFactory {

    private AuthResponseFactory() {
    }

    public static AuthResponseDTO fromUserAndToken(User user, String token, long expiresInSeconds) {
        return new AuthResponseDTO(
                token,
                "Bearer",
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                LocalDateTime.now().plusSeconds(expiresInSeconds)
        );
    }
}

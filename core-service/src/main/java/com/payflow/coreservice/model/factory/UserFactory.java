package com.payflow.coreservice.model.factory;

import com.payflow.coreservice.dto.RegisterRequestDTO;
import com.payflow.coreservice.enums.Enum_User;
import com.payflow.coreservice.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

public final class UserFactory {

    private UserFactory() {
    }

    public static User fromRegisterRequest(
            RegisterRequestDTO request,
            String encodedPassword,
            Function<User.UserBuilder, User.UserBuilder> customizer
    ) {
        User.UserBuilder builder = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .document(request.getDocument())
                .documentType(request.getDocumentType().name())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .status(Enum_User.ACTIVE)
                .createdAt(LocalDateTime.now());

        if (customizer != null) {
            builder = customizer.apply(builder);
        }

        return builder.build();
    }

    public static User fromRegisterRequest(RegisterRequestDTO request, String encodedPassword) {
        return fromRegisterRequest(request, encodedPassword, null);
    }
}

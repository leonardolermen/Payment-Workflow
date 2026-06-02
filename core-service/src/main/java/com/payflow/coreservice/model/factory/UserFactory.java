package com.payflow.coreservice.model.factory;

import com.payflow.commons.dto.user.UserRequest;
import com.payflow.commons.enums.user.User_Status;
import com.payflow.coreservice.dto.RegisterRequestDTO;
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
                .status(User_Status.UNDER_REVIEW)
                .createdAt(LocalDateTime.now());

        if (customizer != null) {
            builder = customizer.apply(builder);
        }

        return builder.build();
    }

    public static User fromRegisterRequest(RegisterRequestDTO request, String encodedPassword) {
        return fromRegisterRequest(request, encodedPassword, null);
    }

    public static User fromUpdateRequest(User user, UserRequest request, String encodedPassword) {
        User.UserBuilder builder = User.builder()
                .name(request.name() != null? request.name(): user.getName())
                .email(request.email() != null? request.email(): user.getEmail())
                .password(encodedPassword != null? encodedPassword: user.getPassword())
                .document(request.document() != null? request.document(): user.getDocument())
                .status(User_Status.ACTIVE);
        return builder.build();
    }
}

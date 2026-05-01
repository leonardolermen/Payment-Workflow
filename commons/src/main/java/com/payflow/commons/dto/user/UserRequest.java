package com.payflow.commons.dto.user;

public record UserRequest(
        String name,
        String email,
        String password,
        String document
) {}

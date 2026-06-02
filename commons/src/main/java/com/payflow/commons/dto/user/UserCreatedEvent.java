package com.payflow.commons.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserCreatedEvent {

    private UUID userId;
    private String name;
    private String email;
    private String document;
    private String documentType;
    private LocalDateTime createdAt;
}

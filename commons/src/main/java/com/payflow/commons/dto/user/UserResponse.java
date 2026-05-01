package com.payflow.commons.dto.user;

import com.payflow.commons.enums.user.User_Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private BigDecimal balance;
    private User_Status status;
    private String document;
    private String documentType;
    private LocalDateTime createdAt;

}

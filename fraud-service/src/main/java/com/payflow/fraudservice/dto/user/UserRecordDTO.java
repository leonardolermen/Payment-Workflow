package com.payflow.fraudservice.dto.user;

import com.payflow.fraudservice.Enums.user.User_Status;
import java.math.BigDecimal;
import java.util.UUID;

public record UserRecordDTO(
        UUID id,
        String name,
        String email,
        BigDecimal balance,
        User_Status status,
        String document,
        String documentType
) {
}

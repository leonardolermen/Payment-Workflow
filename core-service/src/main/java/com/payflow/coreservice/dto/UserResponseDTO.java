package com.payflow.coreservice.dto;

import com.payflow.coreservice.enums.Enum_User;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UserResponseDTO {

    private UUID id;
    private String name;
    private String email;
    private BigDecimal balance;
    private Enum_User status;

}

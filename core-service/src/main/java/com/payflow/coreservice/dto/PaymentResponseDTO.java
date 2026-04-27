package com.payflow.coreservice.dto;

import java.time.LocalDateTime;

import com.payflow.coreservice.enums.Enum_Payment;
import lombok.Data;

@Data
public class PaymentResponseDTO {

    private Long id;
    private Enum_Payment status;
    private LocalDateTime createdAt;

}

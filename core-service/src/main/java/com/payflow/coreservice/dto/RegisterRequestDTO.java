package com.payflow.coreservice.dto;

import com.payflow.coreservice.enums.Document_Type;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterRequestDTO {
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    @Size(min = 6, max = 100)
    private String confirmPassword;

    @NotBlank(message = "Documento é obrigatório")
    private String document;

    @NotBlank(message = "Tipo do documento é obrigatório")
    private Document_Type documentType;

    private BigDecimal balance;

    @AssertTrue(message = "Senhas não conferem")
    public boolean isPasswordMatching(){
        return password != null && password.equals(confirmPassword);
    }
}

package com.payflow.coreservice.dto;
import lombok.Data;

@Data
public class UserRequestDTO {

    private String name;
    private String email;
    private String password;
    private String document;

}

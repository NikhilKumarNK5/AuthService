package org.example.userauthservice.dtos;

import lombok.Data;

@Data
public class LoginRequestDto {
    String email;
    String password;
}

package com.bankManagement.customer_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerLoginRequestDTO {

    @Email
    private String email;

    @NotBlank
    private String password;

}

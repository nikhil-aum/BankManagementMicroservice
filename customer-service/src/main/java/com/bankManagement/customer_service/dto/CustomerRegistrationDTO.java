package com.bankManagement.customer_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegistrationDTO {

    @NotBlank(message="Name is required")
    @Pattern(
       regexp = "^[a-zA-Z ]+$",
       message = "Name should contain only letters and spaces"
    )
    private String name;

    @Email(message = "Enter valid email")
    private String email;

    @Size(min =6 ,message = "Password must contains minimum 6 characters")
    private String password;


}

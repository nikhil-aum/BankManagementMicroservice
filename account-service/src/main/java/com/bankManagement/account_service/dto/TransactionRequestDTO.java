package com.bankManagement.account_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransactionRequestDTO {
    @NotBlank(message = "Account number is required")
    private String accountNumber;


    @NotBlank(message = "Confirm account number is required")
    private String confirmAccountNumber;

    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;


}

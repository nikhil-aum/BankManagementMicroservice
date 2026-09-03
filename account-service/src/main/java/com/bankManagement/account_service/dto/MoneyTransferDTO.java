package com.bankManagement.account_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class MoneyTransferDTO {
    @NotBlank(message = "Sender account number is required")
    private String senderAccountNumber;

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotBlank(message = "Confirm recipient account number is required")
    private String confirmRecipientAccountNumber;

    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;
}

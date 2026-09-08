package com.bankManagement.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

public class AccountDetailsDTO {
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private Long ownerId;
}

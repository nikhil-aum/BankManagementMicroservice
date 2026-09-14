package com.bankManagement.account_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

public class AccountDetailsResponseDTO {
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private Long ownerId;
}

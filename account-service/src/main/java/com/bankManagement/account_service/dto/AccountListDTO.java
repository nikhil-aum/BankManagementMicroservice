package com.bankManagement.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountListDTO {

    private String accountNumber;
    private String holderName;
    private BigDecimal balance;
    private String accountType;
}

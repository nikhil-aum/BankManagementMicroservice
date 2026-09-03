package com.bankManagement.account_service.dto;

import com.bankManagement.account_service.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountDTO {

    @NotNull(message="Account type is required")
    private AccountType accountType;


}

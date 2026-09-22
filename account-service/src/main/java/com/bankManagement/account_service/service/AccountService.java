package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.AccountDetailsResponseDTO;
import com.bankManagement.account_service.dto.AccountListResponseDTO;
import com.bankManagement.account_service.dto.CreateAccountRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;

import java.util.List;

public interface AccountService {
    AccountDetailsResponseDTO createAccount(CreateAccountRequestDTO request, String customerEmail);
    TransactionResponseDTO checkBalance(String accountNumber, String customerEmail);
    List<AccountListResponseDTO> getMyAccounts(String customerEmail);
}
package com.bankManagement.account_service.service;




import com.bankManagement.account_service.dto.AccountDetailsResponseDTO;
import com.bankManagement.account_service.dto.AccountListResponseDTO;
import com.bankManagement.account_service.dto.CreateAccountRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;

import java.util.List;

public interface AccountService {
     AccountDetailsResponseDTO createAccount(CreateAccountRequestDTO request, Long customerId);
     TransactionResponseDTO checkBalance(String accountNumber, Long customerId);
      List<AccountListResponseDTO> getMyAccounts(Long customerId);
}

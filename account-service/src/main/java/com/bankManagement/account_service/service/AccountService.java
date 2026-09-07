package com.bankManagement.account_service.service;




import com.bankManagement.account_service.dto.AccountDetailsDTO;
import com.bankManagement.account_service.dto.AccountListDTO;
import com.bankManagement.account_service.dto.CreateAccountDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;

import java.util.List;

public interface AccountService {
     AccountDetailsDTO createAccount(CreateAccountDTO request, Long customerId);
     TransactionResultDTO checkBalance(String accountNumber, Long customerId);
      List<AccountListDTO> getMyAccounts(Long customerId);
}

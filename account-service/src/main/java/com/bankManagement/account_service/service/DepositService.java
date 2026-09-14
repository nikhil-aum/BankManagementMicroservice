package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;

public interface DepositService {
    TransactionResponseDTO deposit(TransactionRequestDTO request, Long customerId);
}

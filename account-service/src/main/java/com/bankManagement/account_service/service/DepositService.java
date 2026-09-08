package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;

public interface DepositService {
    TransactionResultDTO deposit(TransactionRequestDTO request, Long customerId);
}

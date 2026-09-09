package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;

public interface WithdrawService {
    TransactionResultDTO withdraw(TransactionRequestDTO request, Long customerId);

}

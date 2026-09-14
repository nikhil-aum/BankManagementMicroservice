package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.MoneyTransferRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;

public interface TransferService {
     TransactionResponseDTO transfer(MoneyTransferRequestDTO request, Long customerId);
}

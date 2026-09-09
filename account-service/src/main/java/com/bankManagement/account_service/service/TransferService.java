package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.MoneyTransferDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;

public interface TransferService {
     TransactionResultDTO transfer(MoneyTransferDTO request, Long customerId);
}

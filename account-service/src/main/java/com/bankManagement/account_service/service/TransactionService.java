package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.TransactionHistoryDTO;

import java.util.List;

public interface TransactionService {
     List<TransactionHistoryDTO> getTransactionHistory(
            String accountNumber,
            Long customerId,
            String type,
            String status,
            Double amount,
            String from,
            String to);
}

package com.bankManagement.account_service.service;

import com.bankManagement.account_service.dto.TransactionHistoryResponseDTO;

import java.util.List;

public interface TransactionService {
     List<TransactionHistoryResponseDTO> getTransactionHistory(
             String accountNumber,
             String customerEmail,
             String type,
             String status,
             Double amount,
             String from,
             String to
     );
}
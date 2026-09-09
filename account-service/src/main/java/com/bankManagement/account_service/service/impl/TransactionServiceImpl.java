package com.bankManagement.account_service.service.impl;


import com.bankManagement.account_service.dto.MoneyTransferDTO;
import com.bankManagement.account_service.dto.TransactionHistoryDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.TransactionStatus;
import com.bankManagement.account_service.entity.TransactionType;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.exception.BankingException;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.TransactionService;
import com.bankManagement.account_service.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final AccountRepository accountRepository;

    public TransactionServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public List<TransactionHistoryDTO> getTransactionHistory(
            String accountNumber,
            Long customerId,
            String type,
            String status,
            Double amount,
            String from,
            String to) {

        logger.info("Fetching transaction history for accountNumber={}, customerId={}, type={}, status={}, amount={}, from={}, to={}",
                accountNumber, customerId, type, status, amount, from, to);

        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountNumber);
                    return new BankingException("Wrong account number: " + accountNumber);
                });

        if (!account.getCustomerId().equals(customerId)) {
            logger.warn("Ownership mismatch: accountNumber={} belongs to customerId={}, but request from customerId={}",
                    accountNumber, account.getCustomerId(), customerId);
            throw new AccountOwnershipException();
        }

        List<TransactionHistoryDTO> transactions = account.getTransactions().stream()
                .filter(tx -> {
                    boolean matches = true;

                    if (type != null) {
                        try {
                            TransactionType txnType = TransactionType.valueOf(type.toUpperCase());
                            matches = matches && tx.getType() == txnType;
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid transaction type provided: {}", type);
                            throw new BankingException("Invalid transaction type: " + type);
                        }
                    }

                    if (status != null) {
                        try {
                            TransactionStatus txnStatus = TransactionStatus.valueOf(status.toUpperCase());
                            matches = matches && tx.getStatus() == txnStatus;
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid transaction status provided: {}", status);
                            throw new BankingException("Invalid status: " + status);
                        }
                    }

                    if (amount != null) {
                        matches = matches && tx.getAmount().doubleValue() < amount;
                    }

                    if (from != null && to != null) {
                        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                        LocalTime fromTime = LocalTime.parse(from, timeFmt);
                        LocalTime toTime = LocalTime.parse(to, timeFmt);
                        LocalTime txnTime = tx.getTimestamp().toLocalTime();
                        matches = matches && !txnTime.isBefore(fromTime) && !txnTime.isAfter(toTime);
                    }

                    return matches;
                })
                .map(tx -> new TransactionHistoryDTO(
                        tx.getId(),
                        tx.getType().name(),
                        tx.getAmount(),
                        tx.getTimestamp(),
                        tx.getDescription(),
                        tx.getBalanceAfterTransaction(),
                        tx.getStatus().name()
                ))
                .toList();

        logger.info("Transaction history fetched successfully for accountNumber={}, customerId={}, totalRecords={}",
                accountNumber, customerId, transactions.size());

        return transactions;
    }


}

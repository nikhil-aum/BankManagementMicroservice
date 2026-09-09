package com.bankManagement.account_service.service.impl;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.Transaction;
import com.bankManagement.account_service.entity.TransactionStatus;
import com.bankManagement.account_service.entity.TransactionType;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.exception.BankingException;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.WithdrawService;
import lombok.With;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WithdrawServiceImpl implements WithdrawService {

    private static final Logger logger = LoggerFactory.getLogger(WithdrawServiceImpl.class);

    private final AccountRepository accountRepository;

    public WithdrawServiceImpl(AccountRepository accountRepository){
        this.accountRepository=accountRepository;
    }

    @Override
    public TransactionResultDTO withdraw(TransactionRequestDTO request, Long customerId) {
        logger.info("Withdraw request for account {} by customer {}", request.getAccountNumber(), customerId);

        if (!request.getAccountNumber().equals(request.getConfirmAccountNumber())) {
            logger.error("Account number mismatch: {} & {}",
                    request.getAccountNumber(), request.getConfirmAccountNumber());
            throw new BankingException("Something went wrong: Account numbers do not match");
        }

        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> {
                    logger.error("Account not found with number {}", request.getAccountNumber());
                    return new AccountOwnershipException();
                });

        if (!account.getCustomerId().equals(customerId)) {
            logger.warn("Ownership mismatch for account {} and customer {}", request.getAccountNumber(), customerId);
            throw new AccountOwnershipException();
        }

        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setAmount(request.getAmount());
        transaction.setAccount(account);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            logger.warn("Withdraw failed for account {}. Invalid amount: {}",
                    request.getAccountNumber(), request.getAmount());

            transaction.setDescription("Withdraw failed: Amount must be greater than 0");
            transaction.setBalanceAfterTransaction(account.getBalance());
            transaction.setStatus(TransactionStatus.FAILED);

            account.getTransactions().add(transaction);
            accountRepository.save(account);

            TransactionResultDTO response = new TransactionResultDTO();
            response.setMessage("Withdraw failed: Amount must be greater than 0");
            return response;
        }

        if (request.getAmount().compareTo(account.getBalance()) > 0) {

            logger.warn("Withdrawal failed for account {}. Requested amount {} is greater than balance {}",
                    account.getAccountNumber(), request.getAmount(), account.getBalance());

            transaction.setDescription("Withdrawal failed: Insufficient balance");
            transaction.setBalanceAfterTransaction(account.getBalance());
            transaction.setStatus(TransactionStatus.FAILED);

            account.getTransactions().add(transaction);
            accountRepository.save(account);

            throw new BankingException("Withdrawal failed: Insufficient balance");
        }

        account.withdraw(request.getAmount());
        transaction.setDescription("₹" + request.getAmount() + " debited successfully");
        transaction.setBalanceAfterTransaction(account.getBalance());
        transaction.setStatus(TransactionStatus.SUCCESS);

        account.getTransactions().add(transaction);
        accountRepository.save(account);

        TransactionResultDTO response = new TransactionResultDTO();
        response.setMessage("₹" + request.getAmount() + " debited successfully from your account");
        return response;
    }
}

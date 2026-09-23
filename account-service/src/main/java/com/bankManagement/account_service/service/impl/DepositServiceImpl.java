package com.bankManagement.account_service.service.impl;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.Transaction;
import com.bankManagement.account_service.entity.TransactionStatus;
import com.bankManagement.account_service.entity.TransactionType;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.exception.BankingException;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.DepositService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
@CircuitBreaker(name = "customer-service")
public class DepositServiceImpl implements DepositService {

    private static final Logger logger = LoggerFactory.getLogger(DepositServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;

    @Override
    public TransactionResponseDTO deposit(TransactionRequestDTO request, String customerEmail) {
        logger.info("Deposit request for account {} by email {}", request.getAccountNumber(), customerEmail);

        CustomerExistsResponseDTO customerDto = customerClient.getCustomerByEmail(customerEmail);
        if (customerDto == null || !customerDto.isExists() || customerDto.getCustomerId() == null) {
            logger.error("Customer not found or invalid response for email {}", customerEmail);
            throw new BankingException("Customer not found with email: " + customerEmail);
        }

        Long customerId = customerDto.getCustomerId();

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
            logger.warn("Ownership mismatch for account {} and customer ID {}", request.getAccountNumber(), customerId);
            throw new AccountOwnershipException();
        }

        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setAccount(account);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            logger.warn("Deposit failed for account {}. Invalid amount: {}",
                    request.getAccountNumber(), request.getAmount());

            transaction.setDescription("Deposit failed: Amount must be greater than 0");
            transaction.setBalanceAfterTransaction(account.getBalance());
            transaction.setStatus(TransactionStatus.FAILED);

            account.getTransactions().add(transaction);
            accountRepository.save(account);

            TransactionResponseDTO response = new TransactionResponseDTO();
            response.setMessage("Deposit failed: Amount must be greater than 0");
            return response;
        }

        logger.info("Initiating deposit of ₹{} for account number: {}", request.getAmount(), account.getAccountNumber());

        account.deposit(request.getAmount());
        transaction.setDescription("₹" + request.getAmount() + " credited successfully");
        transaction.setBalanceAfterTransaction(account.getBalance());
        transaction.setStatus(TransactionStatus.SUCCESS);

        account.getTransactions().add(transaction);
        accountRepository.save(account);

        logger.info("Successfully deposited ₹{}. Updated balance for account {}: ₹{}",
                request.getAmount(), account.getAccountNumber(), account.getBalance());

        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setMessage("₹" + request.getAmount() + " credited successfully in your account");
        return response;
    }
}
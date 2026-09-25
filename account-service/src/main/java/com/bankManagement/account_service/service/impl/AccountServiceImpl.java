package com.bankManagement.account_service.service.impl;

import com.bankManagement.account_service.dto.*;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.exception.BankingException;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.AccountService;
import com.bankManagement.account_service.util.CustomerLookupService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@AllArgsConstructor
@CircuitBreaker(name = "customer-service")
public class AccountServiceImpl implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;
    private final CustomerLookupService customerLookupService;

    @Override
    public AccountDetailsResponseDTO createAccount(CreateAccountRequestDTO request, String customerEmail) {
        logger.info("Creating account of type {} for customer email {}", request.getAccountType(), customerEmail);

        CustomerExistsResponseDTO customerDto = customerClient.getCustomerByEmail(customerEmail);

        if (customerDto == null || !customerDto.isExists() || customerDto.getCustomerId() == null) {
            logger.error("Customer not found or invalid response for email {}", customerEmail);
            throw new BankingException("Customer not found with email: " + customerEmail);
        }

        Long customerId = customerDto.getCustomerId();

        if (request.getAccountType() == null) {
            logger.error("Account type is missing for customer email {}", customerEmail);
            throw new BankingException("Account type is required");
        }

        AccountType type = request.getAccountType();
        boolean exist = accountRepository.existsByCustomerIdAndAccountType(customerId, type);

        if (exist) {
            logger.warn("Customer ID {} already has a {} account", customerId, type.name());
            throw new BankingException("Customer already has a " + type.name() + " account");
        }

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setCustomerId(customerId);

        Account savedAccount = accountRepository.save(account);
        logger.info("Account {} created successfully for customer ID {}", savedAccount.getAccountNumber(), customerId);

        AccountDetailsResponseDTO response = new AccountDetailsResponseDTO();
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setAccountType(savedAccount.getAccountType().name());
        response.setBalance(savedAccount.getBalance());
        response.setOwnerId(savedAccount.getCustomerId());

        return response;
    }

    @Override
    public TransactionResponseDTO checkBalance(String accountNumber, String customerEmail) {
        logger.info("Checking balance for account {} requested by email {}", accountNumber, customerEmail);

        CustomerExistsResponseDTO customerDto = customerLookupService.getCustomerByEmail(customerEmail);
        if (customerDto == null || !customerDto.isExists() || customerDto.getCustomerId() == null) {
            throw new BankingException("Customer not found with email: " + customerEmail);
        }

        Long customerId = customerDto.getCustomerId();

        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> {
                    logger.error("Account not found with number {}", accountNumber);
                    return new BankingException("Wrong account number");
                });

        if (!account.getCustomerId().equals(customerId)) {
            logger.warn("Ownership mismatch for account {} and customer ID {}", accountNumber, customerId);
            throw new AccountOwnershipException();
        }

        logger.info("Balance for account {} is {}", accountNumber, account.getBalance());

        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setMessage("Balance in your Account : " + account.getBalance());

        return response;
    }

    @Override
    public List<AccountListResponseDTO> getMyAccounts(String customerEmail) {
        logger.info("Fetching accounts for customer email: {}", customerEmail);

        CustomerExistsResponseDTO customerDto = customerLookupService.getCustomerByEmail(customerEmail);
        if (customerDto == null || !customerDto.isExists() || customerDto.getCustomerId() == null) {
            throw new BankingException("Customer not found with email: " + customerEmail);
        }

        Long customerId = customerDto.getCustomerId();

        List<AccountListResponseDTO> accounts = accountRepository.findByCustomerId(customerId).stream()
                .map(acc -> new AccountListResponseDTO(
                        acc.getAccountNumber(),
                        acc.getCustomerId(),
                        acc.getBalance(),
                        acc.getAccountType().name()
                ))
                .toList();

        if (accounts.isEmpty()) {
            logger.warn("No accounts found for customer ID {}", customerId);
            throw new BankingException("No Accounts Found for customer");
        }

        logger.info("Found {} accounts for customer ID {}", accounts.size(), customerId);
        return accounts;
    }

    private String generateAccountNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}
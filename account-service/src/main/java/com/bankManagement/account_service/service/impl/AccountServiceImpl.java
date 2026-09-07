package com.bankManagement.account_service.service.impl;

import com.bankManagement.account_service.client.CustomerClient;
import com.bankManagement.account_service.dto.*;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.exception.BankingException;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);


    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;

    public AccountServiceImpl(AccountRepository accountRepository,CustomerClient customerClient){
        this.accountRepository = accountRepository;
        this.customerClient = customerClient;
    }

    @Override
    public AccountDetailsDTO createAccount(CreateAccountDTO request, Long customerId) {

        logger.info("Creating account of type {} for customer {}", request.getAccountType(), customerId);

        CustomerExistsDTO  dto = customerClient.customerExists(customerId);
        if (!dto.isExists()) {
            throw new BankingException("Customer not found");
        }

        if (request.getAccountType() == null) {
            logger.error("Account type is missing for customer {}", customerId);
            throw new BankingException("Account type is required");
        }

        AccountType type = request.getAccountType();
        boolean exist = accountRepository.existsByCustomerIdAndAccountType(customerId,type);

        if(exist){
            logger.warn("Customer {} already has a {} account",customerId, type.name());
            throw new BankingException("Customer already has a " +type.name()+"account");
        }
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setCustomerId(customerId);

        Account savedAccount = accountRepository.save(account);
        logger.info("Account {} created successfully for customer {}", savedAccount.getAccountNumber(), customerId);


        AccountDetailsDTO response = new AccountDetailsDTO();
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setAccountType(savedAccount.getAccountType().name());
        response.setBalance(savedAccount.getBalance());
        response.setOwnerId(savedAccount.getCustomerId());

        return response;
    }

    @Override
    public TransactionResultDTO checkBalance(String accountNumber, Long customerId) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> {
                    logger.error("Account not found with number {}", accountNumber);
                    return new BankingException("Wrong account number");
                });

        if (!account.getCustomerId().equals(customerId)) {
            logger.warn("Ownership mismatch for account {} and customer {}", accountNumber, customerId);
            throw new AccountOwnershipException();
        }

        logger.info("Balance for account {} is {}", accountNumber, account.getBalance());

        TransactionResultDTO response = new TransactionResultDTO();
        response.setMessage("Balance in your Account : " + account.getBalance());

        return response;
    }

    @Override
    public List<AccountListDTO> getMyAccounts(Long customerId) {
        List<AccountListDTO> accounts = accountRepository.findByCustomerId(customerId).stream()
                .map(acc -> new AccountListDTO(
                        acc.getAccountNumber(),
                        acc.getCustomerId(),
                        acc.getBalance(),
                        acc.getAccountType().name()
                ))
                .toList();

        if (accounts.isEmpty()) {
            logger.warn("No accounts found for customer {}", customerId);
            throw new BankingException("No Accounts Found for customer " + customerId);
        }

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

package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.AccountDetailsDTO;
import com.bankManagement.account_service.dto.AccountListDTO;
import com.bankManagement.account_service.dto.CreateAccountDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/create")
    public ResponseEntity<AccountDetailsDTO> createAccount(
            @RequestBody CreateAccountDTO request,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Create account request received for customerId: {}", authenticatedCustomerId);



        AccountDetailsDTO account = accountService.createAccount(request, authenticatedCustomerId);

        logger.info("Account created successfully for customerId: {}", authenticatedCustomerId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<TransactionResultDTO> checkBalance(
            @PathVariable String accountNumber,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Balance check request for accountNumber: {} and customerId: {}", accountNumber, authenticatedCustomerId);

        TransactionResultDTO result = accountService.checkBalance(accountNumber, authenticatedCustomerId);

        logger.info("Balance retrieved successfully for accountNumber: {}", accountNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountListDTO>> getMyAccounts(
            @PathVariable Long customerId,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Get accounts request for customerId: {}", authenticatedCustomerId);

        if (!customerId.equals(authenticatedCustomerId)) {
            logger.warn("Ownership mismatch: pathCustomerId={} vs authenticatedCustomerId={}", customerId, authenticatedCustomerId);
            throw new AccountOwnershipException();
        }

        List<AccountListDTO> accounts = accountService.getMyAccounts(authenticatedCustomerId);

        logger.info("Accounts retrieved successfully for customerId: {}", authenticatedCustomerId);
        return ResponseEntity.ok(accounts);
    }


}
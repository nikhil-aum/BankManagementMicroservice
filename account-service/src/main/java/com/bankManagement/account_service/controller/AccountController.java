package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.AccountDetailsResponseDTO;
import com.bankManagement.account_service.dto.AccountListResponseDTO;
import com.bankManagement.account_service.dto.CreateAccountRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.exception.AccountOwnershipException;
import com.bankManagement.account_service.service.AccountService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@AllArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);


    @PostMapping("/create")
    public ResponseEntity<AccountDetailsResponseDTO> createAccount(
            @RequestBody CreateAccountRequestDTO request,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Create account request received for customerId: {}", authenticatedCustomerId);

        AccountDetailsResponseDTO account = accountService.createAccount(request, authenticatedCustomerId);

        logger.info("Account created successfully for customerId: {}", authenticatedCustomerId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<TransactionResponseDTO> checkBalance(
            @PathVariable String accountNumber,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Balance check request for accountNumber: {} and customerId: {}", accountNumber, authenticatedCustomerId);

        TransactionResponseDTO result = accountService.checkBalance(accountNumber, authenticatedCustomerId);

        logger.info("Balance retrieved successfully for accountNumber: {}", accountNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountListResponseDTO>> getMyAccounts(
            @PathVariable Long customerId,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Get accounts request for customerId: {}", authenticatedCustomerId);

        if (!customerId.equals(authenticatedCustomerId)) {
            logger.warn("Ownership mismatch: pathCustomerId={} vs authenticatedCustomerId={}", customerId, authenticatedCustomerId);
            throw new AccountOwnershipException();
        }

        List<AccountListResponseDTO> accounts = accountService.getMyAccounts(authenticatedCustomerId);

        logger.info("Accounts retrieved successfully for customerId: {}", authenticatedCustomerId);
        return ResponseEntity.ok(accounts);
    }


}
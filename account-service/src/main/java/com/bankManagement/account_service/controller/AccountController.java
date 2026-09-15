package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.AccountDetailsResponseDTO;
import com.bankManagement.account_service.dto.AccountListResponseDTO;
import com.bankManagement.account_service.dto.CreateAccountRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.AccountService;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ResponseEntity<?> createAccount(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String customerEmail,
            @RequestBody CreateAccountRequestDTO requestDTO) {

        return ResponseEntity.ok(accountService.createAccount(requestDTO, customerEmail));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> checkBalance(
            @PathVariable String accountNumber,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String customerEmail) {

        return ResponseEntity.ok(accountService.checkBalance(accountNumber, customerEmail));
    }

    @GetMapping("/my-accounts")
    public ResponseEntity<?> getMyAccounts(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String customerEmail) {

        return ResponseEntity.ok(accountService.getMyAccounts(customerEmail));
    }
}
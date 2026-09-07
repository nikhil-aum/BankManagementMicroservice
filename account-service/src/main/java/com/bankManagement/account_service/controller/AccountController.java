package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.AccountDetailsDTO;
import com.bankManagement.account_service.dto.AccountListDTO;
import com.bankManagement.account_service.dto.CreateAccountDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/create/{customerId}")
    public ResponseEntity<AccountDetailsDTO> createAccount(
            @RequestBody CreateAccountDTO request,
            @PathVariable Long customerId) {
        AccountDetailsDTO account = accountService.createAccount(request, customerId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountNumber}/balance/{customerId}")
    public ResponseEntity<TransactionResultDTO> checkBalance(
            @PathVariable String accountNumber,
            @PathVariable Long customerId) {
        TransactionResultDTO result = accountService.checkBalance(accountNumber, customerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountListDTO>> getMyAccounts(@PathVariable Long customerId) {
        List<AccountListDTO> accounts = accountService.getMyAccounts(customerId);
        return ResponseEntity.ok(accounts);
    }
}

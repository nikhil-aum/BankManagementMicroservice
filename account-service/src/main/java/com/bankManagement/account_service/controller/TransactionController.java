package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionHistoryResponseDTO;
import com.bankManagement.account_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Transactions", description = "Transaction APIs")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @GetMapping("/{accountNumber}/transactions")
    @Operation(summary = "Get All Transaction History")
    public ResponseEntity<?> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String authenticatedCustomerEmail) {

        logger.info("Processing transaction history request for accountNumber: {}, email: {}",
                accountNumber, authenticatedCustomerEmail);

        return ResponseEntity.ok(transactionService.getTransactionHistory(
                accountNumber, authenticatedCustomerEmail, type, status, amount, from, to));
    }
}
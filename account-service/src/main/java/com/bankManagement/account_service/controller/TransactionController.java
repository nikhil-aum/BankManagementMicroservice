package com.bankManagement.account_service.controller;


import com.bankManagement.account_service.dto.TransactionHistoryDTO;
import com.bankManagement.account_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Transactions", description = "Transaction APIs")
public class TransactionController {

    private final TransactionService transactionService;

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);


    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @GetMapping("/{accountNumber}/transactions")
    @Operation(summary = "Get All Transaction History")
    public ResponseEntity<List<TransactionHistoryDTO>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {

        logger.info("Transaction history request received for accountNumber={}, customerId={}, type={}, status={}, amount={}, from={}, to={}",
                accountNumber, authenticatedCustomerId, type, status, amount, from, to);

        List<TransactionHistoryDTO> response = transactionService.getTransactionHistory(
                accountNumber, authenticatedCustomerId, type, status, amount, from, to);

        logger.info("Transaction history fetched successfully for accountNumber={}, customerId={}, totalRecords={}",
                accountNumber, authenticatedCustomerId, response.size());

        return ResponseEntity.ok(response);
    }

}

package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposit")
@Tag(name = "Deposit", description = "Deposit APIs")
@AllArgsConstructor
public class DepositController {

    private final DepositService depositService;
    private static final Logger logger = LoggerFactory.getLogger(DepositController.class);

    @PostMapping
    @Operation(summary = "Deposit money")
    public ResponseEntity<TransactionResponseDTO> deposit(
            @RequestBody TransactionRequestDTO request,
            @RequestHeader("X-Customer-Email") String authenticatedCustomerEmail) {

        logger.info("Deposit request received for customer email: {}", authenticatedCustomerEmail);

        TransactionResponseDTO result = depositService.deposit(request, authenticatedCustomerEmail);

        logger.info("Deposit response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }
}
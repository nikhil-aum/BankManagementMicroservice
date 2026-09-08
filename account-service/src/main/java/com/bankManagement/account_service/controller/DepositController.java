package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposit")
@Tag(name = " Deposit", description = "Deposit APIs")
public class DepositController {

    private final DepositService depositService;
    private static final Logger logger = LoggerFactory.getLogger(DepositController.class);


    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @PostMapping
    @Operation(summary = "Deposit money")
    public ResponseEntity<TransactionResultDTO> deposit(@RequestBody TransactionRequestDTO request,
                                                        @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {
        logger.info("Deposit request received for customerId: {}", authenticatedCustomerId);


        TransactionResultDTO result = depositService.deposit(request, authenticatedCustomerId);

        logger.info("Deposit response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }
}

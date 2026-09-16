package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.MoneyTransferRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfer")
@Tag(name = "Transfer", description = "Transfer APIs")
@AllArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);

    @PostMapping
    @Operation(summary = "Transfer money")
    public ResponseEntity<?> transfer(
            @RequestBody MoneyTransferRequestDTO request,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String authenticatedCustomerEmail) {

        logger.info("Processing transfer request for email: {}", authenticatedCustomerEmail);
        return ResponseEntity.ok(transferService.transfer(request, authenticatedCustomerEmail));
    }
}
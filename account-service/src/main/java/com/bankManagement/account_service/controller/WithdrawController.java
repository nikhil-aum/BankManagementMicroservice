package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdraw")
@Tag(name = "Withdraw", description = "Withdraw APIs")
@AllArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;
    private static final Logger logger = LoggerFactory.getLogger(WithdrawController.class);

    @PostMapping
    @Operation(summary = "Withdraw money")
    public ResponseEntity<TransactionResponseDTO> withdraw(
            @RequestBody TransactionRequestDTO request,
            @RequestHeader("X-Customer-Email") String authenticatedCustomerEmail) {

        logger.info("Withdraw request received for customer email: {}", authenticatedCustomerEmail);

        TransactionResponseDTO result = withdrawService.withdraw(request, authenticatedCustomerEmail);

        logger.info("Withdraw response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }
}
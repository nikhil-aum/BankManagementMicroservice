package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ResponseEntity<?> withdraw(
            @RequestBody TransactionRequestDTO request,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Customer-Email", required = false) String authenticatedCustomerEmail) {

        logger.info("Processing withdraw request for email: {}", authenticatedCustomerEmail);
        return ResponseEntity.ok(withdrawService.withdraw(request, authenticatedCustomerEmail));
    }
}
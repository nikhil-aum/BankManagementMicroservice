package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.service.DepositService;
import com.bankManagement.account_service.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdraw")
@Tag(name = "Withdraw", description = "Withdraw APIs")
public class WithdrawController {
    private final WithdrawService withdrawService;
    private static final Logger logger = LoggerFactory.getLogger(WithdrawController.class);


    public WithdrawController(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    @PostMapping
    @Operation(summary = "Withdraw money")
    public ResponseEntity<TransactionResultDTO> withdraw(@RequestBody TransactionRequestDTO request,
                                                        @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {
        logger.info("Withdraw request received for customerId: {}", authenticatedCustomerId);


        TransactionResultDTO result = withdrawService.withdraw(request, authenticatedCustomerId);

        logger.info("Withdraw response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }
}

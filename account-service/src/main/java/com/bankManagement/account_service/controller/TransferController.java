package com.bankManagement.account_service.controller;


import com.bankManagement.account_service.dto.MoneyTransferRequestDTO;
import com.bankManagement.account_service.dto.TransactionResponseDTO;
import com.bankManagement.account_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<TransactionResponseDTO> transfer(@RequestBody MoneyTransferRequestDTO request,
                                                           @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {
        logger.info("Transfer request received for customerId: {}", authenticatedCustomerId);


        TransactionResponseDTO result = transferService.transfer(request, authenticatedCustomerId);

        logger.info("Transfer response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }



}

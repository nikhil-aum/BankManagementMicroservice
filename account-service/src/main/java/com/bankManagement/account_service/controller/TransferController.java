package com.bankManagement.account_service.controller;


import com.bankManagement.account_service.dto.MoneyTransferDTO;
import com.bankManagement.account_service.dto.TransactionRequestDTO;
import com.bankManagement.account_service.dto.TransactionResultDTO;
import com.bankManagement.account_service.service.DepositService;
import com.bankManagement.account_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfer")
@Tag(name = "Transfer", description = "Transfer APIs")
public class TransferController {

    private final TransferService transferService;
    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);

    public TransferController(TransferService transferService){
        this.transferService = transferService;
    }

    @PostMapping
    @Operation(summary = "Transfer money")
    public ResponseEntity<TransactionResultDTO> transfer(@RequestBody MoneyTransferDTO request,
                                                        @RequestHeader("X-Customer-Id") Long authenticatedCustomerId) {
        logger.info("Transfer request received for customerId: {}", authenticatedCustomerId);


        TransactionResultDTO result = transferService.transfer(request, authenticatedCustomerId);

        logger.info("Transfer response: {}", result.getMessage());
        return ResponseEntity.ok(result);
    }



}

package com.bankManagement.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class TransactionHistoryDTO {
    private Long id;
    private String type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;
    private BigDecimal balanceAfterTransaction;
    private String status;
}

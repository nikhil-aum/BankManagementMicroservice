package com.bankManagement.account_service.exception;

public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }
}

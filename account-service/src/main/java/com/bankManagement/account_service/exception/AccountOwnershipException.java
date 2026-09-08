package com.bankManagement.account_service.exception;

public class AccountOwnershipException extends RuntimeException {
    public AccountOwnershipException() {
        super("This account doesn't belong to you...." );
    }
}

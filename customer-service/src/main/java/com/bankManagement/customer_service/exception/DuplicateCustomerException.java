package com.bankManagement.customer_service.exception;

public class DuplicateCustomerException extends RuntimeException{
    public DuplicateCustomerException(String message) {
        super(message);
    }
}

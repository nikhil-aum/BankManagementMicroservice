package com.bankManagement.account_service.exception;


import com.bankManagement.account_service.dto.AccountListResponseDTO;
import com.bankManagement.account_service.service.impl.AccountServiceImpl;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestControllerAdvice
@ControllerAdvice
public class GlobalExceptionHandler {


    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleEnumParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid account type. Allowed values: SAVING, CURRENT");
    }

    @ExceptionHandler(BankingException.class)
    public ResponseEntity<Map<String,String>> handleBankingException(BankingException ex) {
        Map<String,String> response = new HashMap<>();
        response.put("error",ex.getMessage());
        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }


    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getBindingResult().getFieldError().getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParseException(DateTimeParseException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Invalid time format. Please use HH:mm (e.g., 09:00)");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountOwnershipException.class)
    public ResponseEntity<Map<String, String>> handleOwnershipException(AccountOwnershipException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<?> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        logger.warn("Circuit Breaker is OPEN. Short-circuiting request.");

        Map<String, Object> responseBody = createDummyResponseBody(
                "CIRCUIT_OPEN_FALLBACK",
                "Customer Service is currently offline. Returning fallback data."
        );
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(FeignException ex) {
        logger.error("Feign call failed before Circuit Opened. Message: {}", ex.getMessage());

        Map<String, Object> responseBody = createDummyResponseBody(
                "SERVICE_UNAVAILABLE",
                "Unable to connect to Customer Service. Showing temporary fallback data."
        );
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    private Map<String, Object> createDummyResponseBody(String status, String message) {
        AccountListResponseDTO dummyAccount = new AccountListResponseDTO(
                "DUMMY-ACC-9999",
                999L,
                BigDecimal.ZERO,
                "SAVINGS"
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status);
        responseBody.put("message", message);
        responseBody.put("data", List.of(dummyAccount));
        return responseBody;
    }




}

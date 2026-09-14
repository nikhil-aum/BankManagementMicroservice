package com.bankManagement.customer_service.controller;


import com.bankManagement.customer_service.dto.AuthenticationResponseDTO;
import com.bankManagement.customer_service.dto.CustomerLoginRequestDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationRequestDTO;
import com.bankManagement.customer_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    @PostMapping("/register")
    @Operation(summary = "Register new customer")
    public ResponseEntity<String> register(@Valid @RequestBody CustomerRegistrationRequestDTO request){

        logger.info("Register request received for email: {}", request.getEmail());

        authService.register(request);

        logger.info("Customer registered successfully with email: {}", request.getEmail());
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body("Customer registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login customer")
    public ResponseEntity<AuthenticationResponseDTO> login(@Valid @RequestBody CustomerLoginRequestDTO request){

        logger.info("Login attempt for email: {}", request.getEmail());


        String token = authService.login(request);

        logger.info("Login successful for email: {}", request.getEmail());
        return  ResponseEntity.ok(new AuthenticationResponseDTO(token,"Login successful"));
    }
}



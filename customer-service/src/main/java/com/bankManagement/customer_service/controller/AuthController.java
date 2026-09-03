package com.bankManagement.customer_service.controller;


import com.bankManagement.customer_service.dto.AuthenticationResultDTO;
import com.bankManagement.customer_service.dto.CustomerLoginDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationDTO;
import com.bankManagement.customer_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new customer")
    public ResponseEntity<String> register(@Valid @RequestBody CustomerRegistrationDTO request){
        authService.register(request);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body("Customer registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login customer")
    public ResponseEntity<AuthenticationResultDTO> login(@Valid @RequestBody CustomerLoginDTO request){
        String token = authService.login(request);
        return  ResponseEntity.ok(new AuthenticationResultDTO(token,"Login successful"));
    }
}



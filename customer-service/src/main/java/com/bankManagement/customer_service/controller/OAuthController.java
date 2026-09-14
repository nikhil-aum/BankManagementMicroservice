package com.bankManagement.customer_service.controller;

import com.bankManagement.customer_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/oauth2")
@AllArgsConstructor
public class OAuthController {

    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);


    @GetMapping("/google-signUp")
    @Operation(summary = "Auto-register Google OAuth user")
    public ResponseEntity<String> syncGoogleUser(@RequestHeader("X-Customer-Email") String email) {
        logger.info("Google OAuth login sync requested for email: {}", email);

        authService.OAUthSignUp(email);

        logger.info("Google user synced successfully for email: {}", email);
        return ResponseEntity.ok("Google OAuth Authentication Successful. User synced in Database.");
    }
}

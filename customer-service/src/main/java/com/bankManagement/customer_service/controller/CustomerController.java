package com.bankManagement.customer_service.controller;

import com.bankManagement.customer_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.service.CustomerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@AllArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;


    @GetMapping("/me")
    public Customer getProfile(
            @RequestHeader("X-Customer-Email") String email,
            @RequestHeader("X-Customer-Name") String name) {

        return customerService.processGoogleUser(email, name);
    }

    @GetMapping("/by-email")
    public CustomerExistsResponseDTO getCustomerByEmail(@RequestParam("email") String email) {
        log.info("Received internal Feign request for email: {}", email);
        return customerService.getCustomerByEmail(email);
    }
}
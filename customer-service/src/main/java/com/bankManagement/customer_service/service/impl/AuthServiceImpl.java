package com.bankManagement.customer_service.service.impl;

import com.bankManagement.customer_service.dto.CustomerLoginRequestDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationRequestDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.exception.DuplicateCustomerException;
import com.bankManagement.customer_service.exception.InvalidCredentialsException;
import com.bankManagement.customer_service.repository.CustomerRepository;
import com.bankManagement.customer_service.service.AuthService;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);


    private final CustomerRepository repository;

     private final PasswordEncoder encoder;

    private final JwtService jwtService;


    @Override
    public void register(CustomerRegistrationRequestDTO request) {

        logger.info("Registering new customer with email {}", request.getEmail());

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Duplicate registration attempt for email {}", request.getEmail());
            throw new DuplicateCustomerException("Email already registered");
        }
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPassword(encoder.encode(request.getPassword()));
        repository.save(customer);

        logger.info("Customer {} registered successfully", request.getEmail());
    }

    @Override
    public String login(CustomerLoginRequestDTO request) {

        Customer customer = repository.findByEmail(request.getEmail()).orElseThrow(() ->
                new InvalidCredentialsException());

        if(!encoder.matches(request.getPassword(), customer.getPassword())) {

            logger.error("Login failed. Invalid password for email {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(customer.getEmail(), customer.getId());
        logger.info("Login successful for email {}. JWT generated.", request.getEmail());

        return token;
    }

    @Override
    public void OAUthSignUp(String email) {
        logger.info("Processing Google OAuth user sync for email {}", email);

        Optional<Customer> existingCustomer = repository.findByEmail(email);

        if (existingCustomer.isEmpty()) {
            logger.info("Google user not found in DB. Auto-registering user: {}", email);

            Customer newCustomer = new Customer();
            String defaultName = email.contains("@") ? email.split("@")[0] : email;

            newCustomer.setName(defaultName);
            newCustomer.setEmail(email);
            newCustomer.setPassword(encoder.encode("OAUTH2_GOOGLE_USER_NO_PASSWORD"));

            repository.save(newCustomer);
            logger.info("Auto-registration complete for Google user: {}", email);
        } else {
            logger.info("Google user already exists in DB: {}", email);
        }
    }
}
package com.bankManagement.customer_service.service.impl;

import com.bankManagement.customer_service.dto.CustomerLoginDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.exception.DuplicateCustomerException;
import com.bankManagement.customer_service.exception.InvalidCredentialsException;
import com.bankManagement.customer_service.repository.CustomerRepository;
import com.bankManagement.customer_service.service.AuthService;
import com.bankManagement.customer_service.service.impl.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);


    private final CustomerRepository repository;

    private final PasswordEncoder encoder;

    private final JwtService jwtService;

    public AuthServiceImpl(
            CustomerRepository repository,
            PasswordEncoder encoder,
            JwtService jwtService) {

        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Override
    public void register(CustomerRegistrationDTO request) {

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
    public String login(CustomerLoginDTO request) {

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
}
package com.bankManagement.customer_service.service.impl;

import com.bankManagement.customer_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import com.bankManagement.customer_service.service.CustomerService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService{

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository repository;

    @Override
    public Customer processGoogleUser(String email, String name) {
        return repository.findByEmail(email).orElseGet(() -> {
            logger.info("First-time login for email {}. Registering new customer.", email);
            Customer customer = new Customer();
            customer.setEmail(email);
            customer.setName(name != null ? name : "Google User");
            return repository.save(customer);
        });
    }

    @Override
    public CustomerExistsResponseDTO getCustomerByEmail(String email) {
        logger.info("Fetching customer details by email: {}", email);

        Customer customer = processGoogleUser(email, "Google User");

        return new CustomerExistsResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                true
        );
    }
}
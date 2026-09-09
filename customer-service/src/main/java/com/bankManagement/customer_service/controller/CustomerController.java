package com.bankManagement.customer_service.controller;


import com.bankManagement.customer_service.dto.CustomerExistsDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(
            CustomerRepository repository) {

        this.repository = repository;
    }

    @GetMapping("/{id}/exists")
    public CustomerExistsDTO exists(@PathVariable Long id) {
        logger.info("Received request to check existence of customer with ID: {}", id);

        boolean exists = repository.existsById(id);

        if (exists) {
            Customer customer = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found despite existsById true"));
            logger.info("Customer with ID {} exists in the database", id);
            return new CustomerExistsDTO(id, customer.getName(), true);
        } else {
            logger.warn("Customer with ID {} does not exist in the database", id);
            return new CustomerExistsDTO(id, null, false);
        }
    }

}
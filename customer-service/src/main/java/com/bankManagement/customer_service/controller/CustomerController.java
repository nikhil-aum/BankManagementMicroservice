package com.bankManagement.customer_service.controller;


import com.bankManagement.customer_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@AllArgsConstructor
public class CustomerController {

    private final CustomerRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @GetMapping("/{id}/exists")
    public CustomerExistsResponseDTO exists(@PathVariable Long id) {
        logger.info("Received request to check existence of customer with ID: {}", id);

        boolean exists = repository.existsById(id);

        if (exists) {
            Customer customer = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found despite existsById true"));
            logger.info("Customer with ID {} exists in the database", id);
            return new CustomerExistsResponseDTO(id, customer.getName(), true);
        } else {
            logger.warn("Customer with ID {} does not exist in the database", id);
            return new CustomerExistsResponseDTO(id, null, false);
        }
    }

}
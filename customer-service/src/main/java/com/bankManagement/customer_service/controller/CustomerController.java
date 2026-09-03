package com.bankManagement.customer_service.controller;


import com.bankManagement.customer_service.dto.CustomerExistsDTO;
import com.bankManagement.customer_service.repository.CustomerRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(
            CustomerRepository repository) {

        this.repository = repository;
    }

    @GetMapping("/{id}/exists")
    public CustomerExistsDTO exists(@PathVariable Long id){
        return new CustomerExistsDTO(id, repository.existsById(id));
    }
}
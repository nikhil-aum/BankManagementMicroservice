package com.bankManagement.customer_service.service;

import com.bankManagement.customer_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.customer_service.entity.Customer;

public interface CustomerService {

    Customer processGoogleUser(String email, String name);
    CustomerExistsResponseDTO getCustomerByEmail(String email);
}

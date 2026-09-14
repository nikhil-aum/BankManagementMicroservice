package com.bankManagement.customer_service.service;


import com.bankManagement.customer_service.dto.CustomerLoginRequestDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationRequestDTO;

public interface AuthService {
    void register(CustomerRegistrationRequestDTO request);
    String login(CustomerLoginRequestDTO request);
}

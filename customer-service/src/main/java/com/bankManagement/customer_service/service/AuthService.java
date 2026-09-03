package com.bankManagement.customer_service.service;


import com.bankManagement.customer_service.dto.CustomerLoginDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationDTO;

public interface AuthService {
    void register(CustomerRegistrationDTO request);
    String login(CustomerLoginDTO request);
}

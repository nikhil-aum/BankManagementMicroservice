package com.bankManagement.account_service.util;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.feign.CustomerClient;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomerLookupService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerLookupService.class);

    private final CustomerClient customerClient;

    @Retry(name = "customer-service")
    public CustomerExistsResponseDTO getCustomerByEmail(String email) {
        logger.debug("Looking up customer by email: {}", email);
        return customerClient.getCustomerByEmail(email);
    }
}
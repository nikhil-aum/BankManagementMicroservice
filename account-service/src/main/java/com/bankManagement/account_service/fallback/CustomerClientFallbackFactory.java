package com.bankManagement.account_service.fallback;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.feign.CustomerClient;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    @Override
    public CustomerClient create(Throwable cause) {
        return new CustomerClient() {
            @Override
            public CustomerExistsResponseDTO getCustomerByEmail(String email) {

                if (cause instanceof FeignException) {
                    throw (FeignException) cause;
                }
                throw new RuntimeException("Customer Service unavailable: " + cause.getMessage(), cause);
            }
        };
    }
}
package com.bankManagement.account_service.feign;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/by-email")
    CustomerExistsResponseDTO getCustomerByEmail(@RequestParam("email") String email);
}
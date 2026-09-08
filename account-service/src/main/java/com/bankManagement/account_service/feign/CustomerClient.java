package com.bankManagement.account_service.feign;

import com.bankManagement.account_service.dto.CustomerExistsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}/exists")
    CustomerExistsDTO customerExists(@PathVariable("id") Long id);
}

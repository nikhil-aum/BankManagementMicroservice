package com.bankManagement.customer_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerExistsDTO {

    private Long customerId;

    private String customerName;

    private boolean exists;

}
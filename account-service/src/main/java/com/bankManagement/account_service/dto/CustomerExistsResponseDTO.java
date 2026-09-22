package com.bankManagement.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerExistsResponseDTO {

    private Long customerId;
    private String name;
    private String email;
    private boolean exists;
}
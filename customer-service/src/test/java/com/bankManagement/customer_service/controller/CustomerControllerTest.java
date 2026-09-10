package com.bankManagement.customer_service.controller;

import com.bankManagement.customer_service.CustomerServiceApplication;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        },
        classes = CustomerServiceApplication .class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void customerExist_success() throws Exception{
        Customer customer = new Customer();
        customer.setName("Nikhil Patidar");
        customer.setEmail("nikhil@gmail.com");
        customer.setPassword("password123");
        repository.save(customer);

        mockMvc.perform(get("/api/customers/" + customer.getId() + "/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.customerName").value("Nikhil Patidar"))
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void customer_notExist() throws Exception {

        mockMvc.perform(get("/api/customers/999/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(999))
                .andExpect(jsonPath("$.customerName").isEmpty())
                .andExpect(jsonPath("$.exists").value(false));
    }
}

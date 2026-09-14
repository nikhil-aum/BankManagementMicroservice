package com.bankManagement.customer_service.controller;

import com.bankManagement.customer_service.dto.CustomerLoginRequestDTO;
import com.bankManagement.customer_service.dto.CustomerRegistrationRequestDTO;
import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void register_Success() throws Exception {

        CustomerRegistrationRequestDTO request = new CustomerRegistrationRequestDTO();
        request.setName("Mukesh Kumar");
        request.setEmail("mukesh@gmail.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Customer registered successfully"));
    }

    @Test
    void login_Success() throws Exception {


        CustomerRegistrationRequestDTO registerRequest = new CustomerRegistrationRequestDTO();
        registerRequest.setName("Rahul Kumar");
        registerRequest.setEmail("rahul@gmail.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        CustomerLoginRequestDTO loginRequest = new CustomerLoginRequestDTO();
        loginRequest.setEmail("rahul@gmail.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.message")
                        .value("Login successful"));
    }

    @Test
    void register_InvalidEmail() throws Exception {

        CustomerRegistrationRequestDTO request = new CustomerRegistrationRequestDTO();
        request.setName("Nikhil");
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword() throws Exception {

        CustomerRegistrationRequestDTO request = new CustomerRegistrationRequestDTO();
        request.setName("Nikhil");
        request.setEmail("test@gmail.com");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithWrongPassword() throws Exception {



        CustomerRegistrationRequestDTO registerRequest = new CustomerRegistrationRequestDTO();
        registerRequest.setName("Nikhil");
        registerRequest.setEmail("nikhil@gmail.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        CustomerLoginRequestDTO loginRequest = new CustomerLoginRequestDTO();
        loginRequest.setEmail("nikhil@gmail.com");
        loginRequest.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_DuplicateEmail() throws Exception {

        Customer customer = new Customer();
        customer.setName("Vikesh");
        customer.setEmail("vikesh@gmail.com");
        customer.setPassword("hgh8900");

        customerRepository.save(customer);

        CustomerRegistrationRequestDTO request = new CustomerRegistrationRequestDTO();
        request.setName("Vikesh");
        request.setEmail("vikesh@gmail.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
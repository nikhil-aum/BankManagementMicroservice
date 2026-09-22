package com.bankManagement.customer_service.controller;

import com.bankManagement.customer_service.entity.Customer;
import com.bankManagement.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
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
    void getProfile_existingCustomer() throws Exception {
        Customer existingCustomer = new Customer();
        existingCustomer.setName("Nikhil Patidar");
        existingCustomer.setEmail("nikhil@gmail.com");
        repository.save(existingCustomer);

        mockMvc.perform(get("/api/customers/me")
                        .header("X-Customer-Email", "nikhil@gmail.com")
                        .header("X-Customer-Name", "Nikhil Patidar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCustomer.getId()))
                .andExpect(jsonPath("$.email").value("nikhil@gmail.com"))
                .andExpect(jsonPath("$.name").value("Nikhil Patidar"));
    }

    @Test
    void newCustomer_createsAndReturnsCustomer() throws Exception {

        mockMvc.perform(get("/api/customers/me")
                        .header("X-Customer-Email", "vikas@gmail.com")
                        .header("X-Customer-Name", "Vikas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value("vikas@gmail.com"))
                .andExpect(jsonPath("$.name").value("Vikas"));
    }

    @Test
    @WithMockUser
    void getCustomerByEmail_existingCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setName("Mohan verma");
        customer.setEmail("mohan@gmail.com");
        customer = repository.save(customer);

        mockMvc.perform(get("/api/customers/by-email")
                        .param("email", "mohan@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.name").value("Mohan verma"))
                .andExpect(jsonPath("$.email").value("mohan@gmail.com"))
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @WithMockUser
    void getCustomerByEmail_createsAndReturnsDTO() throws Exception {
        mockMvc.perform(get("/api/customers/by-email")
                        .param("email", "ram@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", notNullValue()))
                .andExpect(jsonPath("$.name").value("Google User"))
                .andExpect(jsonPath("$.email").value("ram@gmail.com"))
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @WithMockUser
    void getCustomerByEmail_missingEmailParam() throws Exception {
        mockMvc.perform(get("/api/customers/by-email")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
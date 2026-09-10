package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CreateAccountDTO;
import com.bankManagement.account_service.dto.CustomerExistsDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("account-test")
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerClient customerClient;

    @Autowired
    private AccountRepository accountRepository;


    @Test
    void createAccount_success() throws Exception {
        when(customerClient.customerExists(1L))
                .thenReturn(new CustomerExistsDTO(1L, "Nikhil", true));

        CreateAccountDTO request = new CreateAccountDTO();
        request.setAccountType(AccountType.SAVING);

        mockMvc.perform(post("/api/accounts/create")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("SAVING"))
                .andExpect(jsonPath("$.accountNumber").exists());
    }

    @Test
    void createAccount_nullAccountType() throws Exception {
        when(customerClient.customerExists(1L))
                .thenReturn(new CustomerExistsDTO(1L, "Nikhil", true));

        CreateAccountDTO request = new CreateAccountDTO();
        request.setAccountType(null);

        mockMvc.perform(post("/api/accounts/create")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_MissingHeader() throws Exception {
        when(customerClient.customerExists(1L))
                .thenReturn(new CustomerExistsDTO(1L, "Nikhil", true));

        CreateAccountDTO request = new CreateAccountDTO();
        request.setAccountType(AccountType.CURRENT);

        mockMvc.perform(post("/api/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }



    @Test
    void checkBalance_Success() throws Exception {
        when(customerClient.customerExists(1L))
                .thenReturn(new CustomerExistsDTO(1L, "Nikhil", true));

        Account account = new Account();
        account.setAccountNumber("ACC12345");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));
        account.setCustomerId(1L);
        accountRepository.save(account);


        mockMvc.perform(get("/api/accounts/{accountNumber}/balance", "ACC12345")
                        .header("X-Customer-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Balance in your Account : 5000.00"));

    }

    @Test
    void checkBalance_MissingHeader() throws Exception {
        String accountNumber = "ACC12345";

        mockMvc.perform(get("/api/accounts/{accountNumber}/balance", accountNumber))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void checkBalance_InvalidAccount_ShouldReturnError() throws Exception {
        when(customerClient.customerExists(1L))
                .thenReturn(new CustomerExistsDTO(1L, "Nikhil", true));

        String invalidAccountNumber = "76786849";

        mockMvc.perform(get("/api/accounts/{accountNumber}/balance", invalidAccountNumber)
                        .header("X-Customer-Id", 1L))
                .andExpect(status().is4xxClientError());
    }


    }

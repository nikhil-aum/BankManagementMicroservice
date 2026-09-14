package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
class DepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;


    @Test
    void deposit_Success() throws Exception {

        Account account = new Account();
        account.setAccountNumber("ACC12345");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));
        account.setCustomerId(1L);

        accountRepository.save(account);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("₹2000 credited successfully in your account"));
    }



    @Test
    void deposit_NegativeAmount() throws Exception {

        Account account = new Account();
        account.setAccountNumber("ACC12345");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));
        account.setCustomerId(1L);

        accountRepository.save(account);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": -1000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Deposit failed: Amount must be greater than 0"));
    }




    @Test
    void deposit_AccountNumberMismatch() throws Exception {

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC99999",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }



    @Test
    void deposit_AccountNotFound() throws Exception {

        String requestBody = """
                {
                    "accountNumber": "INVALID123",
                    "confirmAccountNumber": "INVALID123",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }




    @Test
    void deposit_OwnershipMismatch() throws Exception {

        Account account = new Account();
        account.setAccountNumber("ACC12345");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));

        account.setCustomerId(1L);

        accountRepository.save(account);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }



    @Test
    void deposit_MissingCustomerIdHeader() throws Exception {

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

}
package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.util.CustomerLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private CustomerLookupService customerLookupService;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void deposit_Success() throws Exception {
        String email = "nikhil@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(1L, "Nikhil", email, true);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

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
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("₹2000 credited successfully in your account"));
    }

    @Test
    void deposit_NegativeAmount() throws Exception {
        String email = "nikhil@gmail.com";

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": -1000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_ZeroAmount() throws Exception {
        String email = "nikhil@gmail.com";

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 0
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_BlankAccountNumber() throws Exception {
        String email = "nikhil@gmail.com";

        String requestBody = """
                {
                    "accountNumber": "",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_BlankConfirmAccountNumber() throws Exception {
        String email = "nikhil@gmail.com";

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_AccountNumberMismatch() throws Exception {
        String email = "nikhil@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(1L, "Nikhil", email, true);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC99999",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_AccountNotFound() throws Exception {
        String email = "nikhil@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(1L, "Nikhil", email, true);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

        String requestBody = """
                {
                    "accountNumber": "INVALID123",
                    "confirmAccountNumber": "INVALID123",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void deposit_OwnershipMismatch() throws Exception {
        String email = "nikhil@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(2L, "Nikhil", email, true);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

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
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void deposit_CustomerNotFound_NotExists() throws Exception {
        String email = "unknown@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(null, null, email, false);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_CustomerNullResponse() throws Exception {
        String email = "nullcustomer@gmail.com";

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(null);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_CustomerIdNull() throws Exception {
        String email = "nullid@gmail.com";

        CustomerExistsResponseDTO customer =
                new CustomerExistsResponseDTO(null, "Nikhil", email, true);

        when(customerLookupService.getCustomerByEmail(email)).thenReturn(customer);

        String requestBody = """
                {
                    "accountNumber": "ACC12345",
                    "confirmAccountNumber": "ACC12345",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/deposit")
                        .header("X-Customer-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_MissingCustomerEmailHeader() throws Exception {
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
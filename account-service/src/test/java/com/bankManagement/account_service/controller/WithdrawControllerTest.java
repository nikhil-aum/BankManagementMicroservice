
package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
class WithdrawControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private CustomerClient customerClient;

    @BeforeEach
    void setUp() {

        accountRepository.deleteAll();

        reset(customerClient);

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("customer-service");

        circuitBreaker.transitionToClosedState();
    }

    private String requestBody(
            String accountNumber,
            String confirmAccountNumber,
            String amount) {

        return """
                {
                    "accountNumber": "%s",
                    "confirmAccountNumber": "%s",
                    "amount": %s
                }
                """.formatted(
                accountNumber,
                confirmAccountNumber,
                amount
        );
    }

    private CustomerExistsResponseDTO customerExists(
            boolean exists,
            Long customerId) {

        CustomerExistsResponseDTO response =
                new CustomerExistsResponseDTO();

        response.setExists(exists);
        response.setCustomerId(customerId);

        return response;
    }

    private Account createAccount(
            String accountNumber,
            Long customerId,
            BigDecimal balance) {

        Account account = new Account();

        account.setAccountNumber(accountNumber);
        account.setAccountType(AccountType.SAVING);
        account.setBalance(balance);
        account.setCustomerId(customerId);

        return accountRepository.saveAndFlush(account);
    }

    @Test
    void withdraw_Success() throws Exception {

        createAccount(
                "ACC12345",
                1L,
                BigDecimal.valueOf(10000)
        );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "₹2000 debited successfully from your account"
                                )
                );
    }

    @Test
    void withdraw_NegativeAmount() throws Exception {

        createAccount(
                "ACC12345",
                1L,
                BigDecimal.valueOf(10000)
        );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "-1000"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Withdraw failed: Amount must be greater than 0"
                                )
                );
    }

    @Test
    void withdraw_ZeroAmount() throws Exception {

        createAccount(
                "ACC12345",
                1L,
                BigDecimal.valueOf(10000)
        );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "0"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Withdraw failed: Amount must be greater than 0"
                                )
                );
    }

    @Test
    void withdraw_InsufficientBalance() throws Exception {

        createAccount(
                "ACC12345",
                1L,
                BigDecimal.valueOf(1000)
        );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Withdrawal failed: Insufficient balance"
                                )
                );
    }

    @Test
    void withdraw_AccountNumberMismatch() throws Exception {

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC99999",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Something went wrong: Account numbers do not match"
                                )
                );
    }

    @Test
    void withdraw_AccountNotFound() throws Exception {

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "INVALID123",
                                                "INVALID123",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void withdraw_OwnershipMismatch() throws Exception {

        createAccount(
                "ACC12345",
                999L,
                BigDecimal.valueOf(10000)
        );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerExists(true, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "nikhil@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void withdraw_CustomerNotFound() throws Exception {

        when(customerClient.getCustomerByEmail("unknown@gmail.com"))
                .thenReturn(customerExists(false, null));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "unknown@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Customer not found with email: unknown@gmail.com"
                                )
                );
    }

    @Test
    void withdraw_CustomerResponseNull() throws Exception {

        when(customerClient.getCustomerByEmail("null@gmail.com"))
                .thenReturn(null);

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "null@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Customer not found with email: null@gmail.com"
                                )
                );
    }

    @Test
    void withdraw_CustomerIdNull() throws Exception {

        when(customerClient.getCustomerByEmail("invalid@gmail.com"))
                .thenReturn(customerExists(true, null));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "invalid@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Customer not found with email: invalid@gmail.com"
                                )
                );
    }

    @Test
    void withdraw_CustomerExistsFalse() throws Exception {

        when(customerClient.getCustomerByEmail("notexists@gmail.com"))
                .thenReturn(customerExists(false, 1L));

        mockMvc.perform(
                        post("/api/withdraw")
                                .header(
                                        "X-Customer-Email",
                                        "notexists@gmail.com"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        requestBody(
                                                "ACC12345",
                                                "ACC12345",
                                                "2000"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Customer not found with email: notexists@gmail.com"
                                )
                );
    }
}

package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.entity.Transaction;
import com.bankManagement.account_service.entity.TransactionStatus;
import com.bankManagement.account_service.entity.TransactionType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
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
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
class TransactionControllerTest {

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

        circuitBreakerRegistry
                .circuitBreaker("customer-service")
                .reset();

        CustomerExistsResponseDTO customerResponse =
                new CustomerExistsResponseDTO(
                        1L,
                        "Nikhil",
                        "nikhil@gmail.com",
                        true
                );

        when(customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .thenReturn(customerResponse);
    }

    private Account createAccount() {

        Account account = new Account();

        account.setAccountNumber("ACC10001");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(10000));
        account.setCustomerId(1L);

        Transaction transaction1 = new Transaction();

        transaction1.setAccount(account);
        transaction1.setType(TransactionType.DEPOSIT);
        transaction1.setAmount(BigDecimal.valueOf(5000));
        transaction1.setTimestamp(
                LocalDateTime.of(2026, 9, 11, 10, 30)
        );
        transaction1.setDescription("₹5000 deposited successfully");
        transaction1.setBalanceAfterTransaction(
                BigDecimal.valueOf(15000)
        );
        transaction1.setStatus(TransactionStatus.SUCCESS);

        Transaction transaction2 = new Transaction();

        transaction2.setAccount(account);
        transaction2.setType(TransactionType.WITHDRAW);
        transaction2.setAmount(BigDecimal.valueOf(2000));
        transaction2.setTimestamp(
                LocalDateTime.of(2026, 9, 11, 12, 30)
        );
        transaction2.setDescription("₹2000 withdrawn successfully");
        transaction2.setBalanceAfterTransaction(
                BigDecimal.valueOf(13000)
        );
        transaction2.setStatus(TransactionStatus.SUCCESS);

        account.getTransactions().add(transaction1);
        account.getTransactions().add(transaction2);

        return accountRepository.save(account);
    }

    @Test
    void getTransactionHistory_Success() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(5000))
                .andExpect(jsonPath("$[1].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[1].amount").value(2000));
    }

    @Test
    void getTransactionHistory_AccountNotFound() throws Exception {

        mockMvc.perform(
                        get("/api/accounts/ACC99999/transactions")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_OwnershipMismatch() throws Exception {

        Account account = createAccount();
        account.setCustomerId(999L);
        accountRepository.save(account);

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionHistory_byType() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("type", "DEPOSIT")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(5000));
    }

    @Test
    void getTransactionHistory_byStatus() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("status", "SUCCESS")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTransactionHistory_byAmount() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("amount", "3000")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(2000));
    }

    @Test
    void getTransactionHistory_byTime() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("from", "09:00")
                                .param("to", "17:00")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(5000))
                .andExpect(jsonPath("$[1].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[1].amount").value(2000));
    }


    @Test
    void getTransactionHistory_InvalidType() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("type", "INVALID")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_InvalidStatus() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("status", "INVALID")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_WrongTimeFormat() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .param("from", "10:30:00")
                                .param("to", "12:00")
                                .header("X-Customer-Email", "nikhil@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_CustomerNotFound() throws Exception {

        when(customerClient.getCustomerByEmail("unknown@gmail.com"))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                null,
                                null,
                                "unknown@gmail.com",
                                false
                        )
                );

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .header("X-Customer-Email", "unknown@gmail.com")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_MissingCustomerEmail() throws Exception {

        createAccount();

        mockMvc.perform(
                        get("/api/accounts/ACC10001/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }
}
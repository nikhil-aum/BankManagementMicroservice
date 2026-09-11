package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.entity.*;
import com.bankManagement.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
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

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

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

        mockMvc.perform(get("/api/accounts/ACC99999/transactions")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_OwnershipMismatch() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .header("X-Customer-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionHistory_byType() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("type", "DEPOSIT")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(5000));
    }

    @Test
    void getTransactionHistory_byStatus() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("status", "SUCCESS")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTransactionHistory_byAmount() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("amount", "3000")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(2000));
    }

    @Test
    void getTransactionHistory_byTime() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("from", "15:00")
                        .param("to", "20:00")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    @Test
    void getTransactionHistory_InvalidType() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("type", "INVALID")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_InvalidStatus() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("status", "INVALID")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionHistory_WrongTimeFormat() throws Exception {

        createAccount();

        mockMvc.perform(get("/api/accounts/ACC10001/transactions")
                        .param("from", "10:30:00")
                        .param("to", "12:00")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest());
    }
}
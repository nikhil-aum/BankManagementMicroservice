package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CustomerExistsDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.entity.TransactionStatus;
import com.bankManagement.account_service.entity.TransactionType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test1")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private CustomerClient customerClient;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }


    @Test
    void transfer_Success() throws Exception {

        Account sender = new Account();
        sender.setAccountNumber("ACC10001");
        sender.setAccountType(AccountType.SAVING);
        sender.setBalance(BigDecimal.valueOf(10000));
        sender.setCustomerId(1L);

        Account receiver = new Account();
        receiver.setAccountNumber("ACC20001");
        receiver.setAccountType(AccountType.SAVING);
        receiver.setBalance(BigDecimal.valueOf(5000));
        receiver.setCustomerId(2L);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        CustomerExistsDTO senderCustomer = new CustomerExistsDTO();
        senderCustomer.setCustomerName("Nikhil");

        CustomerExistsDTO receiverCustomer = new CustomerExistsDTO();
        receiverCustomer.setCustomerName("Rahul");

        when(customerClient.customerExists(1L))
                .thenReturn(senderCustomer);

        when(customerClient.customerExists(2L))
                .thenReturn(receiverCustomer);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("₹2000 transferred successfully from Nikhil to Rahul"));
    }



    @Test
    void transfer_RecipientConfirmationMismatch() throws Exception {

        Account sender = new Account();
        sender.setAccountNumber("ACC10001");
        sender.setAccountType(AccountType.SAVING);
        sender.setBalance(BigDecimal.valueOf(10000));
        sender.setCustomerId(1L);

        Account receiver = new Account();
        receiver.setAccountNumber("ACC20001");
        receiver.setAccountType(AccountType.SAVING);
        receiver.setBalance(BigDecimal.valueOf(5000));
        receiver.setCustomerId(2L);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC99999",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Something went wrong....."));
    }




    @Test
    void transfer_SenderAccountNotFound() throws Exception {

        String requestBody = """
            {
                "senderAccountNumber": "ACC99999",
                "recipientAccountNumber": "ACC20001",
                "confirmRecipientAccountNumber": "ACC20001",
                "amount": 2000
            }
            """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("This account doesn't belong to you...."));
    }



    @Test
    void transfer_SenderOwnershipMismatch() throws Exception {

        Account sender = new Account();
        sender.setAccountNumber("ACC10001");
        sender.setAccountType(AccountType.SAVING);
        sender.setBalance(BigDecimal.valueOf(10000));
        sender.setCustomerId(10L);

        Account receiver = new Account();
        receiver.setAccountNumber("ACC20001");
        receiver.setAccountType(AccountType.SAVING);
        receiver.setBalance(BigDecimal.valueOf(5000));
        receiver.setCustomerId(2L);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        String requestBody = """
            {
                "senderAccountNumber": "ACC10001",
                "recipientAccountNumber": "ACC20001",
                "confirmRecipientAccountNumber": "ACC20001",
                "amount": 2000
            }
            """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("This account doesn't belong to you...."));
    }




    @Test
    void transfer_SameSenderAndRecipientAccount() throws Exception {

        Account account = new Account();
        account.setAccountNumber("ACC10001");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(10000));
        account.setCustomerId(1L);

        accountRepository.save(account);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC10001",
                    "confirmRecipientAccountNumber": "ACC10001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }


    @Test
    void transfer_NegativeAmount() throws Exception {

        Account sender = new Account();
        sender.setAccountNumber("ACC10001");
        sender.setAccountType(AccountType.SAVING);
        sender.setBalance(BigDecimal.valueOf(10000));
        sender.setCustomerId(1L);

        Account receiver = new Account();
        receiver.setAccountNumber("ACC20001");
        receiver.setAccountType(AccountType.SAVING);
        receiver.setBalance(BigDecimal.valueOf(5000));
        receiver.setCustomerId(2L);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": -500
                }
                """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }


    @Test
    void transfer_InsufficientBalance() throws Exception {

        Account sender = new Account();
        sender.setAccountNumber("ACC10001");
        sender.setAccountType(AccountType.SAVING);

        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCustomerId(1L);

        Account receiver = new Account();
        receiver.setAccountNumber("ACC20001");
        receiver.setAccountType(AccountType.SAVING);
        receiver.setBalance(BigDecimal.valueOf(5000));
        receiver.setCustomerId(2L);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(post("/api/transfer")
                        .header("X-Customer-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

}
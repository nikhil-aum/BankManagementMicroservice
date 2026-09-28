
package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
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
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private CustomerClient customerClient;


    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }


    @Test
    void transfer_Success() throws Exception {

        String senderEmail = "nikhil@gmail.com";
        String receiverEmail = "rahul@gmail.com";

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

        CustomerExistsResponseDTO senderCustomer =
                new CustomerExistsResponseDTO(
                        1L,
                        "Nikhil",
                        senderEmail,
                        true
                );

        CustomerExistsResponseDTO receiverCustomer =
                new CustomerExistsResponseDTO(
                        2L,
                        "Rahul",
                        receiverEmail,
                        true
                );

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(senderCustomer);

        when(customerClient.getCustomerByEmail(receiverEmail))
                .thenReturn(receiverCustomer);

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("₹2000 transferred successfully from Nikhil to Account ACC20001")
                );
    }


    @Test
    void transfer_RecipientConfirmationMismatch() throws Exception {

        String senderEmail = "nikhil@gmail.com";

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

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC99999",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value("Something went wrong: Recipient account numbers do not match")
                );
    }


    @Test
    void transfer_SenderAccountNotFound() throws Exception {

        String senderEmail = "nikhil@gmail.com";

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC99999",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.error")
                                .value("This account doesn't belong to you....")
                );
    }


    @Test
    void transfer_SenderOwnershipMismatch() throws Exception {

        String senderEmail = "nikhil@gmail.com";

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

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.error")
                                .value("This account doesn't belong to you....")
                );
    }


    @Test
    void transfer_SameSenderAndRecipientAccount() throws Exception {

        String senderEmail = "nikhil@gmail.com";

        Account account = new Account();
        account.setAccountNumber("ACC10001");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(10000));
        account.setCustomerId(1L);

        accountRepository.save(account);

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC10001",
                    "confirmRecipientAccountNumber": "ACC10001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void transfer_NegativeAmount() throws Exception {

        String senderEmail = "nikhil@gmail.com";

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

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": -500
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void transfer_InsufficientBalance() throws Exception {

        String senderEmail = "nikhil@gmail.com";

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

        when(customerClient.getCustomerByEmail(senderEmail))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                senderEmail,
                                true
                        )
                );

        String requestBody = """
                {
                    "senderAccountNumber": "ACC10001",
                    "recipientAccountNumber": "ACC20001",
                    "confirmRecipientAccountNumber": "ACC20001",
                    "amount": 2000
                }
                """;

        mockMvc.perform(
                        post("/api/transfer")
                                .header("X-Customer-Email", senderEmail)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }
}

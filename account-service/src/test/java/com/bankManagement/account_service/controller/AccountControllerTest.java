package com.bankManagement.account_service.controller;

import com.bankManagement.account_service.dto.CreateAccountRequestDTO;
import com.bankManagement.account_service.dto.CustomerExistsResponseDTO;
import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import com.bankManagement.account_service.feign.CustomerClient;
import com.bankManagement.account_service.repository.AccountRepository;
import com.bankManagement.account_service.service.AccountService;
import com.bankManagement.account_service.util.CustomerLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@ActiveProfiles("test1")
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerClient customerClient;

    @MockBean
    private CustomerLookupService customerLookupService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;


    @Test
    void createAccount_success() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerClient.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(AccountType.SAVING);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .header("X-Customer-Email", email)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("SAVING"))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.ownerId").value(1));
    }


    @Test
    void createAccount_nullAccountType() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerClient.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(null);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .header("X-Customer-Email", email)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void createAccount_MissingHeader() throws Exception {

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(AccountType.CURRENT);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void createAccount_CustomerNotFound() throws Exception {

        String email = "unknown@gmail.com";

        when(customerClient.getCustomerByEmail(email))
                .thenReturn(null);

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(AccountType.SAVING);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .header("X-Customer-Email", email)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void createAccount_CustomerDoesNotExist() throws Exception {

        String email = "unknown@gmail.com";

        when(customerClient.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                99L,
                                "Unknown",
                                email,
                                false
                        )
                );

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(AccountType.SAVING);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .header("X-Customer-Email", email)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void createAccount_CustomerAlreadyHasAccountType() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerClient.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        Account existingAccount = new Account();
        existingAccount.setAccountNumber("111111111111");
        existingAccount.setAccountType(AccountType.SAVING);
        existingAccount.setBalance(BigDecimal.ZERO);
        existingAccount.setCustomerId(1L);

        accountRepository.save(existingAccount);

        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setAccountType(AccountType.SAVING);

        mockMvc.perform(
                        post("/api/accounts/create")
                                .header("X-Customer-Email", email)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void checkBalance_Success() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        Account account = new Account();
        account.setAccountNumber("ACC12345");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));
        account.setCustomerId(1L);

        accountRepository.save(account);

        mockMvc.perform(
                        get("/api/accounts/{accountNumber}/balance", "ACC12345")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Balance in your Account : 5000.00")
                );
    }


    @Test
    void checkBalance_MissingHeader() throws Exception {

        mockMvc.perform(
                        get("/api/accounts/{accountNumber}/balance", "ACC12345")
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void checkBalance_InvalidAccount_ShouldReturnError() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        mockMvc.perform(
                        get("/api/accounts/{accountNumber}/balance", "76786849")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void checkBalance_OwnershipMismatch() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        Account account = new Account();
        account.setAccountNumber("ACC99999");
        account.setAccountType(AccountType.SAVING);
        account.setBalance(BigDecimal.valueOf(5000));
        account.setCustomerId(2L);

        accountRepository.save(account);

        mockMvc.perform(
                        get("/api/accounts/{accountNumber}/balance", "ACC99999")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void checkBalance_CustomerNotFound() throws Exception {

        String email = "unknown@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/accounts/{accountNumber}/balance", "ACC12345")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getMyAccounts_Success() throws Exception {

        String email = "nikhil@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                1L,
                                "Nikhil",
                                email,
                                true
                        )
                );

        Account savingAccount = new Account();
        savingAccount.setAccountNumber("111111111111");
        savingAccount.setAccountType(AccountType.SAVING);
        savingAccount.setBalance(BigDecimal.valueOf(5000));
        savingAccount.setCustomerId(1L);

        Account currentAccount = new Account();
        currentAccount.setAccountNumber("222222222222");
        currentAccount.setAccountType(AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.valueOf(12000));
        currentAccount.setCustomerId(1L);

        accountRepository.save(savingAccount);
        accountRepository.save(currentAccount);

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").exists())
                .andExpect(jsonPath("$[0].accountType").exists())
                .andExpect(jsonPath("$[0].balance").exists())
                .andExpect(jsonPath("$[1].accountNumber").exists())
                .andExpect(jsonPath("$[1].accountType").exists())
                .andExpect(jsonPath("$[1].balance").exists());
    }


    @Test
    void getMyAccounts_MultipleAccountsForSameCustomer() throws Exception {

        String email = "customer6@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                6L,
                                "Customer Six",
                                email,
                                true
                        )
                );

        Account savingAccount = new Account();
        savingAccount.setAccountNumber("111111111111");
        savingAccount.setAccountType(AccountType.SAVING);
        savingAccount.setBalance(BigDecimal.valueOf(500));
        savingAccount.setCustomerId(6L);

        Account currentAccount = new Account();
        currentAccount.setAccountNumber("222222222222");
        currentAccount.setAccountType(AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.valueOf(1200));
        currentAccount.setCustomerId(6L);

        accountRepository.save(savingAccount);
        accountRepository.save(currentAccount);

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(6))
                .andExpect(jsonPath("$[1].customerId").value(6));
    }


    @Test
    void getMyAccounts_NoAccountsFound() throws Exception {

        String email = "noaccount@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                10L,
                                "No Account Customer",
                                email,
                                true
                        )
                );

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getMyAccounts_MissingHeader() throws Exception {

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getMyAccounts_CustomerNotFound() throws Exception {

        String email = "unknown@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void getMyAccounts_CustomerExistsFalse() throws Exception {

        String email = "inactive@gmail.com";

        when(customerLookupService.getCustomerByEmail(email))
                .thenReturn(
                        new CustomerExistsResponseDTO(
                                99L,
                                "Inactive Customer",
                                email,
                                false
                        )
                );

        mockMvc.perform(
                        get("/api/accounts/my-accounts")
                                .header("X-Customer-Email", email)
                )
                .andExpect(status().isBadRequest());
    }
}

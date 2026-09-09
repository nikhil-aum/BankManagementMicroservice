package com.bankManagement.customer_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRegister_success() throws Exception {
        String requestJson = """
                {
                  "name":"Ram",
                  "email":"ram12@gmail.com",
                  "password":"ram@123" 
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ram",
                                  "email":"ram@example.com",
                                  "password":"ram123"
                                }
                                """))
                .andExpect(status().isCreated());
    }


    @Test
    void testLogin_success() throws Exception{
        String registerJson = """
            {
              "name":"vikas",
              "email":"vikas@gmail.com",
              "password":"vikas123"
            }
        """;
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());


        String loginJson = """
            {
              "email":"vikas@gmail.com",
              "password":"vikas123"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.token").exists())
                .andExpect((ResultMatcher) jsonPath("$.message").value("Login successful"));
    }

}

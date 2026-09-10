package com.bankManagement.customer_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = CustomerServiceApplication.class)
@ActiveProfiles("test")
class CustomerServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
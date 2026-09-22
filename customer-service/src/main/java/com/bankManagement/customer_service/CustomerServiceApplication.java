package com.bankManagement.customer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
public class CustomerServiceApplication {

	public static void main(String[] args) {
		System.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3307/bankManagement_customer_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
		System.setProperty("spring.datasource.username", "root");
		System.setProperty("spring.datasource.password", "Admin@123");
		SpringApplication.run(CustomerServiceApplication.class, args);
	}

}

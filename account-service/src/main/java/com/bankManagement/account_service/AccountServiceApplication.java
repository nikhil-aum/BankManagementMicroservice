package com.bankManagement.account_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class AccountServiceApplication {

	public static void main(String[] args) {
		System.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3307/bankManagement_account_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
		System.setProperty("spring.datasource.username", "root");
		System.setProperty("spring.datasource.password", "Admin@123");
		SpringApplication.run(AccountServiceApplication.class, args);
	}

}

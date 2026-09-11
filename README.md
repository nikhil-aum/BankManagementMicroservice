# MiniBank — Microservices Banking System

A secure, distributed banking backend built by transforming the Phase 4 MiniBank monolith into an independently deployable microservices architecture.

The system keeps the original banking business rules unchanged while separating responsibilities across dedicated services. Service discovery, centralized configuration, API routing, JWT-based security, inter-service communication, independent databases, and integration testing are used to demonstrate a production-style microservices design.

> **Learning note:** For an application of this size, a monolith is completely reasonable in a real-world environment. This microservices version is intentionally built to learn service boundaries, service discovery, API gateway routing, centralized configuration, and service-to-service communication.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Services](#services)
- [Project Structure](#project-structure)
- [Service Responsibilities](#service-responsibilities)
- [Technology Stack](#technology-stack)
- [Service Ports](#service-ports)
- [Database-per-Service](#database-per-service)
- [Domain Boundaries](#domain-boundaries)
- [Customer Service](#customer-service)
- [Account Service](#account-service)
- [API Gateway](#api-gateway)
- [Authentication and Authorization](#authentication-and-authorization)
- [JWT Flow](#jwt-flow)
- [Service Discovery with Eureka](#service-discovery-with-eureka)
- [Inter-Service Communication](#inter-service-communication)
- [Centralized Configuration](#centralized-configuration)
- [Gateway Routing](#gateway-routing)
- [API Endpoints](#api-endpoints)
- [Business Rules](#business-rules)
- [Customer Registration Rules](#customer-registration-rules)
- [Account Rules](#account-rules)
- [Deposit Rules](#deposit-rules)
- [Withdrawal Rules](#withdrawal-rules)
- [Transfer Rules](#transfer-rules)
- [Transaction History](#transaction-history)
- [Validation](#validation)
- [Exception Handling](#exception-handling)
- [HTTP Status Codes](#http-status-codes)
- [OpenAPI / Swagger](#openapi--swagger)
- [Logging](#logging)
- [Configuration and Environment Variables](#configuration-and-environment-variables)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Build and Run](#build-and-run)
- [Startup Order](#startup-order)
- [API Testing Flow](#api-testing-flow)
- [Integration Testing](#integration-testing)
- [H2 Test Database](#h2-test-database)
- [Failure Handling](#failure-handling)
- [Important Microservices Design Decisions](#important-microservices-design-decisions)
- [Monolith vs Microservices](#monolith-vs-microservices)
- [Common Errors](#common-errors)
- [Example End-to-End Flow](#example-end-to-end-flow)
- [Future Improvements](#future-improvements)
- [Author](#author)
- [Version History](#version-history)
- [License](#license)
- [Acknowledgments](#acknowledgments)

---

# Project Overview

MiniBank Phase 5 breaks the previous banking monolith into independently deployable services.

The system is organized as:

```text
                              ┌──────────────────────┐
                              │       Client         │
                              │ Postman / Swagger    │
                              └──────────┬───────────┘
                                         │
                                         │ HTTP
                                         ▼
                              ┌──────────────────────┐
                              │     API Gateway      │
                              │       :8080          │
                              │ Routing + JWT Auth    │
                              │     / Authorization  │
                              └──────────┬───────────┘
                                         │
                           ┌─────────────┴─────────────┐
                           │                           │
                           ▼                           ▼
                ┌────────────────────┐       ┌────────────────────┐
                │ Customer Service   │       │  Account Service  │
                │      :8081        │       │      :8082        │
                │                    │       │                    │
                │ Customer           │       │ Account            │
                │ Registration       │       │ Transaction        │
                │ Login              │       │ Deposit            │
                │ JWT issuing        │       │ Withdrawal         │
                │                    │       │ Transfer            │
                └─────────┬──────────┘       │ Balance             │
                          │                  │ History             │
                          │                  └─────────┬──────────┘
                          │                            │
                          ▼                            ▼
                ┌────────────────────┐       ┌────────────────────┐
                │ Customer Database  │       │ Account Database   │
                │     MySQL          │       │      MySQL          │
                └────────────────────┘       └────────────────────┘

                          ▲
                          │
                 ┌────────┴────────┐
                 │ Discovery Server│
                 │     Eureka      │
                 │      :8761      │
                 └─────────────────┘

                 Centralized Configuration
                 ┌─────────────────────┐
                 │    Config Server    │
                 │  Shared properties  │
                 └─────────────────────┘
```

The external client communicates with the system through the API Gateway. Internal services communicate through service names discovered using Eureka rather than hardcoded host/port addresses.

---

# Architecture

The application follows a microservices architecture while preserving layered architecture inside each business service.

Each business service maintains:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
Own Database
```

The distributed architecture is:

```text
Client
  │
  ▼
API Gateway
  │
  ├──────────────► customer-service
  │                    │
  │                    ▼
  │              Customer DB
  │
  └──────────────► account-service
                       │
                       ▼
                  Account DB
                       │
                       └── Transaction data

        customer-service ◄──── OpenFeign ──── account-service

                    All services
                         │
                         ▼
                      Eureka

                    All services
                         │
                         ▼
                   Config Server
```

---

# Services

The project contains the following independently runnable Maven applications:

```text
minibank-microservices/
│
├── config-server/
│
├── discovery-server/
│
├── api-gateway/
│
├── customer-service/
│
└── account-service/
```

### Service Summary

| Service | Responsibility | Port |
|---|---|---:|
| `config-server` | Centralized application configuration | 8888 |
| `discovery-server` | Eureka service registry | 8761 |
| `api-gateway` | Routing, authentication and authorization | 8080 |
| `customer-service` | Customer, registration, login and JWT issuing | 8081 |
| `account-service` | Account, transactions and banking operations | 8082 |

Each service is independently buildable and runnable.

---

# Project Structure

```text
minibank-microservices
│
├── config-server
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java
│           │   └── ...ConfigServerApplication.java
│           └── resources
│               └── application.yml
│
├── discovery-server
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java
│           │   └── ...DiscoveryServerApplication.java
│           └── resources
│               └── application.yml
│
├── api-gateway
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── ...
│       │   │       ├── config
│       │   │       ├── security
│       │   │       ├── filter
│       │   │       └── exception
│       │   └── resources
│       │       └── application.yml
│       │
│       └── test
│           └── java
│
├── customer-service
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── ...
│       │   │       ├── controller
│       │   │       ├── service
│       │   │       ├── repository
│       │   │       ├── entity
│       │   │       ├── dto
│       │   │       ├── exception
│       │   │       ├── config
│       │   │       └── utility
│       │   └── resources
│       │       └── application.yml
│       │
│       └── test
│           └── java
│
└── account-service
    ├── pom.xml
    └── src
        ├── main
        │   ├── java
        │   │   └── ...
        │   │       ├── controller
        │   │       ├── service
        │   │       ├── repository
        │   │       ├── entity
        │   │       ├── dto
        │   │       ├── exception
        │   │       ├── config
        │   │       └── client
        │   └── resources
        │       └── application.yml
        │
        └── test
            └── java
```

> The exact package names may vary according to the implementation. The important architectural rule is that each service keeps its own controller/service/repository/entity/dto/exception/config layers.

---

# Service Responsibilities

## 1. Config Server

The Config Server provides centralized configuration for the microservices.

Configuration that can be centralized includes:

- Service names
- Eureka configuration
- Database configuration
- JWT configuration
- Gateway configuration
- Application-specific properties
- Common environment-driven settings

The goal is to avoid duplicating configuration across services.

Secrets should still be supplied securely through environment variables or a secure configuration mechanism.

---

## 2. Discovery Server

The Discovery Server is implemented using Spring Cloud Netflix Eureka.

Responsibilities:

- Maintain the service registry
- Allow services to register themselves
- Allow services to discover other services
- Remove the need for hardcoded peer host/port values

The Discovery Server runs on:

```text
http://localhost:8761
```

---

## 3. API Gateway

The API Gateway is the single entry point for clients.

Responsibilities:

- Route incoming requests
- Authenticate requests using JWT
- Apply authorization/security rules
- Forward requests to the appropriate service
- Use Eureka service IDs for routing
- Return a clean 404 when no route matches

The gateway runs on:

```text
http://localhost:8080
```

Business logic such as balance validation, insufficient-fund checks and account ownership remains inside the appropriate business service.

---

## 4. Customer Service

Customer Service owns the customer domain.

Responsibilities:

- Customer registration
- Customer validation
- Duplicate email checking
- Password hashing
- Login
- JWT issuing
- Customer existence verification
- Customer database ownership

Customer Service does not own account or transaction tables.

---

## 5. Account Service

Account Service owns the account and transaction domains.

Responsibilities:

- Account creation
- Account balance
- Customer account listing
- Deposit
- Withdrawal
- Transfer
- Transaction history
- Transaction filtering
- Account ownership validation
- Account and transaction database ownership

Account and Transaction remain together deliberately because a transfer changes two accounts and creates double-entry transaction records.

Splitting Account and Transaction into separate services would introduce distributed transaction complexity, which is outside the scope of this project.

---

# Technology Stack

| Technology | Usage |
|---|---|
| Java 21 | Application development |
| Spring Boot 4.0.7 | Application framework |
| Spring Web MVC | REST APIs |
| Spring Data JPA | Persistence |
| Hibernate | ORM |
| MySQL | Production/runtime databases |
| Spring Security | Security |
| JWT | Authentication token |
| BCrypt | Password hashing |
| Spring Cloud Netflix Eureka | Service discovery |
| Spring Cloud Gateway | API Gateway |
| Spring Cloud OpenFeign | Service-to-service communication |
| Spring Cloud Config | Centralized configuration |
| Jakarta Bean Validation | Request validation |
| springdoc OpenAPI | API documentation |
| JUnit 5 | Integration test framework |
| Spring Boot Test | Application integration testing |
| MockMvc | HTTP/API integration testing |
| H2 | Test database |
| Maven | Build and dependency management |
| Lombok | Boilerplate reduction |
| Postman | Manual API testing |

> **Testing note:** This Phase 5 project focuses on integration testing. The README intentionally does not describe the Phase 4 Mockito-based unit-test suite as the primary testing strategy.

---

# Service Ports

| Component | Port | Purpose |
|---|---:|---|
| API Gateway | 8080 | External API entry point |
| Customer Service | 8081 | Customer APIs |
| Account Service | 8082 | Account and banking APIs |
| Eureka Server | 8761 | Service registry |
| Config Server | 8888 | Centralized configuration |

The client should normally use only:

```text
http://localhost:8080
```

Direct service ports are mainly useful for development and debugging.

---

# Database-per-Service

A major microservices rule in this project is:

> **One service owns one database.**

The architecture is:

```text
customer-service
      │
      ▼
Customer Database
      │
      └── customers


account-service
      │
      ▼
Account Database
      │
      ├── accounts
      └── transactions
```

Customer Service must never query Account Service's database.

Account Service must never query Customer Service's database.

There are no cross-service JPA relationships.

---

# Domain Boundaries

## Customer Service Owns

```text
Customer
```

Customer-related responsibilities:

```text
Registration
Login
Password hashing
JWT issuing
Customer existence
```

## Account Service Owns

```text
Account
Transaction
```

Account-related responsibilities:

```text
Account creation
Balance
Deposit
Withdrawal
Transfer
Transaction history
Transaction filtering
Ownership checks
```

---

# Customer Service

Customer Service contains the customer domain and authentication credential logic.

## Customer Entity

Typical customer information includes:

```text
id
name
email
password
createdDate
```

The password stored in the database is BCrypt encoded.

The plain-text password is never persisted.

---

# Account Service

Account Service contains:

```text
Account
Transaction
AccountType
TransactionType
TransactionStatus
```

The Account entity no longer has:

```java
@ManyToOne
Customer customer
```

because Customer belongs to another service and another database.

Instead, Account stores a plain customer identifier:

```text
customerId
```

Example:

```text
Account
--------------------------------
accountNumber
balance
accountType
customerId
createdDate
```

This is a service boundary identifier, not a JPA relationship.

---

# API Gateway

The Gateway provides a single public API.

```text
Client
  │
  ▼
Gateway :8080
  │
  ├── /api/auth/**       → customer-service
  ├── /api/customers/**  → customer-service
  └── /api/accounts/**   → account-service
```

The Gateway is responsible for:

- Routing
- JWT authentication
- Authorization/security filtering
- Handling invalid/unauthenticated requests
- Returning clean routing errors

The Gateway is **not** responsible for:

- Account balance calculation
- Deposit business rules
- Withdrawal business rules
- Transfer business rules
- Account ownership business logic
- Transaction creation

Those rules remain in Account Service.

---

# Authentication and Authorization

Authentication-related gateway code is located in the API Gateway.

The authentication architecture is:

```text
Customer
   │
   ▼
POST /api/auth/login
   │
   ▼
Customer Service
   │
   ▼
JWT generated
   │
   ▼
Client receives JWT
   │
   ▼
Client sends:
Authorization: Bearer <JWT>
   │
   ▼
API Gateway
   │
   ▼
JWT authentication / authorization
   │
   ├── Invalid / missing token
   │          │
   │          ▼
   │      401 Unauthorized
   │
   └── Valid token
              │
              ▼
        Request routed
              │
              ▼
       Target Microservice
```

JWT issuing remains part of Customer Service because Customer Service owns customer authentication credentials.

The Gateway performs the external request security check before forwarding protected requests.

---

# JWT Flow

```text
1. Customer registers
          ↓
2. Customer Service stores BCrypt password
          ↓
3. Customer logs in
          ↓
4. Customer Service validates credentials
          ↓
5. Customer Service generates JWT
          ↓
6. Client receives JWT
          ↓
7. Client sends Bearer token to Gateway
          ↓
8. Gateway validates JWT
          ↓
9. Valid request is routed
          ↓
10. Account Service processes banking operation
```

Protected request:

```http
Authorization: Bearer <JWT_TOKEN>
```

The JWT should contain enough information for downstream ownership/security decisions, such as the authenticated customer identifier.

---

# Shared JWT Secret

For this educational implementation, services that need to validate the JWT use the same signing secret supplied through configuration/environment variables.

The secret must never be hardcoded in Java source code.

Example:

```text
JWT_SECRET=<secure-secret>
```

This shared-secret approach is a simplification for the project.

A production architecture would commonly use asymmetric cryptography:

```text
Customer Service
      │
      │ signs
      ▼
Private Key
      │
      ▼
JWT
      │
      ▼
Gateway / Other Services
      │
      │ verifies
      ▼
Public Key
```

This avoids distributing a signing secret to every verifier.

---

# Ownership Authorization

Account ownership remains the responsibility of Account Service.

The rule is:

```text
Authenticated Customer ID
          │
          ▼
Account.customerId
          │
          ▼
Compare
          │
    ┌─────┴─────┐
    │           │
   Match      Mismatch
    │           │
    ▼           ▼
 Allow       403 Forbidden
```

A customer cannot access another customer's account or transaction history.

The ownership rule is enforced downstream where the account data actually exists.

---

# Service Discovery with Eureka

The project uses Spring Cloud Netflix Eureka.

Each service registers using its logical application name.

Example logical service names:

```text
customer-service
account-service
api-gateway
```

The important principle is:

```text
Do not hardcode:

http://localhost:8081
http://localhost:8082
```

inside service-to-service application code.

Instead:

```text
account-service
      │
      ▼
Eureka
      │
      ▼
customer-service
```

The service name is resolved through service discovery.

---

# Inter-Service Communication

Account Service needs to confirm that a customer exists when opening an account.

Customer Service exposes an internal endpoint:

```http
GET /api/customers/{id}/exists
```

The response is intentionally minimal.

Example:

```json
{
  "exists": true
}
```

No password, password hash or unnecessary customer information is returned.

---

# OpenFeign

Account Service uses Spring Cloud OpenFeign for communication with Customer Service.

Conceptually:

```text
Account Service
      │
      │ OpenFeign
      ▼
customer-service
      │
      ▼
GET /api/customers/{id}/exists
```

The Feign client uses the Eureka service name rather than a hardcoded host and port.

Example conceptual service identifier:

```text
customer-service
```

This keeps service location dynamic.

---

# Customer Existence Check

The customer existence check happens when creating an account.

Flow:

```text
POST /api/accounts/open
          │
          ▼
Validate account request
          │
          ▼
Read authenticated customerId
          │
          ▼
Check Customer locally? ── NO
          │
          ▼
Call customer-service through OpenFeign
          │
          ▼
Customer exists?
     ┌────┴────┐
    YES        NO
     │          │
     ▼          ▼
Create      AccountNotFoundException
Account
```

This remote check happens only when the account is opened.

Deposits, withdrawals, transfers and balance checks do not repeatedly call Customer Service because Account Service already has the customer ID associated with the account.

This reduces unnecessary network dependency and latency.

---

# Centralized Configuration

The project uses a Config Server to centralize configuration.

Instead of keeping all configuration independently duplicated in every service, services retrieve their configuration through the Config Server.

Typical centralized properties include:

```text
spring.application.name
server.port
database settings
Eureka settings
JWT settings
Gateway routes
service-specific configuration
```

A simplified flow:

```text
Service
   │
   ▼
Config Server :8888
   │
   ▼
Configuration
   │
   ▼
Service starts with externalized properties
```

Secrets should be supplied securely through environment variables or a secure secret-management mechanism.

---

# Gateway Routing

The Gateway exposes the following logical routes:

| Incoming Path | Destination |
|---|---|
| `/api/auth/**` | customer-service |
| `/api/customers/**` | customer-service |
| `/api/accounts/**` | account-service |

Conceptually:

```text
/api/auth/**
       │
       ▼
lb://customer-service


/api/customers/**
       │
       ▼
lb://customer-service


/api/accounts/**
       │
       ▼
lb://account-service
```

`lb://` indicates that the destination is resolved through service discovery/load balancing.

The client does not need to know the internal service addresses.

---

# API Endpoints

All client-facing endpoints are accessed through the Gateway.

Base URL:

```text
http://localhost:8080
```

## Authentication APIs

| Method | Endpoint | Authentication | Service |
|---|---|---|---|
| POST | `/api/auth/register` | No | customer-service |
| POST | `/api/auth/login` | No | customer-service |

## Customer APIs

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| GET | `/api/customers/{id}/exists` | Internal/service call | Check customer existence |

> The customer-existence endpoint is intended for Account Service communication and should not expose sensitive customer data.

## Account APIs

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| POST | `/api/accounts/open` | Yes | Open account |
| GET | `/api/accounts/{accountNumber}` | Yes | Check balance |
| GET | `/api/accounts/my-accounts` | Yes | Get logged-in customer's accounts |

## Banking APIs

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| POST | `/api/deposit` | Yes | Deposit money |
| POST | `/api/withdraw` | Yes | Withdraw money |
| POST | `/api/transfer` | Yes | Transfer money |

## Transaction History APIs

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| GET | `/api/accounts/{accountNumber}/history` | Yes | All transactions |
| GET | `/api/accounts/{accountNumber}/deposit-history` | Yes | Deposit history |
| GET | `/api/accounts/{accountNumber}/withdraw-history` | Yes | Withdrawal history |
| GET | `/api/accounts/{accountNumber}/transferIn-history` | Yes | Transfer-in history |
| GET | `/api/accounts/{accountNumber}/transferOut-history` | Yes | Transfer-out history |
| GET | `/api/accounts/{amount}/balance-history` | Yes | Balance-based history |
| GET | `/api/accounts/time-history` | Yes | Time-based history |
| GET | `/api/accounts/status-history` | Yes | Status-based history |

---

# Business Rules

The microservices migration does not change the original banking rules.

The rules from the earlier MiniBank phases remain enforced.

---

# Customer Registration Rules

- Customer name is required.
- Customer name can contain only letters and spaces.
- Email must be valid.
- Password must contain at least 6 characters.
- Email must be unique.
- Duplicate email registration returns `409 Conflict`.
- Password is encoded using BCrypt.
- Plain-text passwords are never stored.
- Passwords must never be logged.

---

# Account Rules

- Account type is required.
- Supported account types are:

```text
SAVING
CURRENT
```

- A customer can have at most one SAVING account.
- A customer can have at most one CURRENT account.
- A new account starts with balance `0`.
- Account numbers are generated as 12-digit numeric strings.
- The customer must exist before an account is created.
- Account ownership is enforced using `customerId`.
- Account Service does not use a cross-database JPA relationship.

---

# Deposit Rules

- Account number is required.
- Confirmation account number is required.
- Account number and confirmation account number must match.
- Deposit amount must be greater than zero.
- The authenticated customer must own the account.
- Successful deposits increase account balance.
- A `DEPOSIT` transaction is recorded.
- Invalid deposit attempts follow the existing failure-handling rules.

Example request:

```http
POST /api/deposit
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "accountNumber": "123456789012",
  "confirmAccountNumber": "123456789012",
  "amount": 5000
}
```

---

# Withdrawal Rules

- Account number is required.
- Confirmation account number is required.
- Account number and confirmation account number must match.
- Withdrawal amount must be greater than zero.
- The authenticated customer must own the account.
- Withdrawal cannot exceed available balance.
- Successful withdrawal decreases the balance.
- A `WITHDRAW` transaction is recorded.
- Failed insufficient-balance withdrawals are recorded according to the existing transaction rules.

---

# Transfer Rules

- Sender account number is required.
- Recipient account number is required.
- Confirmation recipient account number is required.
- Recipient and confirmation recipient account numbers must match.
- Sender and recipient accounts must be different.
- Transfer amount must be greater than zero.
- Sender must have sufficient balance.
- Sender ownership must be validated.
- Successful transfer creates:
  - `TRANSFER_OUT` transaction for sender.
  - `TRANSFER_IN` transaction for recipient.
- Failed transfers caused by invalid amount or insufficient funds follow the existing failed-transaction rules.

---

# Transfer and Transaction Boundary

Account and Transaction intentionally remain in the same service:

```text
account-service
     │
     ├── Account
     │
     └── Transaction
```

A transfer changes:

```text
Sender Account
     +
Recipient Account
     +
Sender Transaction
     +
Recipient Transaction
```

Keeping these objects inside one service/database allows the operation to remain within a local database transaction boundary.

Splitting them into different services would introduce distributed transaction requirements, which are intentionally outside the scope of this project.

---

# Transaction History

Account Service provides transaction history and filtering.

Supported transaction types:

```text
DEPOSIT
WITHDRAW
TRANSFER_IN
TRANSFER_OUT
```

Supported statuses:

```text
SUCCESS
FAILED
```

Transaction information includes fields such as:

```text
id
type
amount
timestamp
description
balanceAfterTransaction
status
account
```

---

# Transaction History APIs

## Complete History

```http
GET /api/accounts/{accountNumber}/history
```

Returns all transactions for an account after ownership validation.

## Deposit History

```http
GET /api/accounts/{accountNumber}/deposit-history
```

Returns:

```text
type = DEPOSIT
```

## Withdrawal History

```http
GET /api/accounts/{accountNumber}/withdraw-history
```

Returns:

```text
type = WITHDRAW
```

## Transfer-In History

```http
GET /api/accounts/{accountNumber}/transferIn-history
```

Returns:

```text
type = TRANSFER_IN
```

## Transfer-Out History

```http
GET /api/accounts/{accountNumber}/transferOut-history
```

Returns:

```text
type = TRANSFER_OUT
```

## Balance-Based History

```http
GET /api/accounts/{amount}/balance-history
```

Transactions are selected where:

```text
balanceAfterTransaction < amount
```

The supplied amount must be greater than zero.

## Time-Based History

```http
GET /api/accounts/time-history?from=09:00&to=18:00
```

Expected time format:

```text
HH:mm
```

Example:

```text
09:00
18:30
```

The time range is inclusive.

## Status-Based History

```http
GET /api/accounts/status-history?status=SUCCESS
```

Supported statuses:

```text
SUCCESS
FAILED
```

Status input is handled case-insensitively.

---

# Validation

The project uses Jakarta Bean Validation.

Typical validations include:

```java
@NotBlank
@Email
@Pattern
@Size(min = 6)
@NotNull
@Positive
```

Validation covers:

- Customer name
- Email
- Password
- Account type
- Account number
- Confirmation account number
- Transaction amount
- Transfer fields
- Status/time input where applicable

---

# Exception Handling

Each business service maintains its own exception-handling structure.

Typical exceptions include:

```text
AccountNotFoundException
AccountOwnershipException
DuplicateCustomerException
InvalidCredentialsException
BankingException
```

Validation and malformed input are also handled consistently.

The Gateway handles security/routing-related failures separately from banking business exceptions.

---

# HTTP Status Codes

| Status | Meaning |
|---|---|
| `200 OK` | Successful request |
| `201 Created` | Resource successfully created where applicable |
| `400 Bad Request` | Validation/business/input error |
| `401 Unauthorized` | Missing/invalid/expired authentication |
| `403 Forbidden` | Authenticated user is not authorized to access the resource |
| `404 Not Found` | Resource or route not found |
| `409 Conflict` | Duplicate/conflicting resource |

A request that matches no Gateway route returns a clean `404 Not Found`.

---

# OpenAPI / Swagger

The project can use springdoc OpenAPI for API documentation where configured.

Because the Gateway is the external entry point, the public API should be tested through:

```text
http://localhost:8080
```

Swagger UI, when exposed by the application, can be used to:

- View API documentation
- Inspect request/response DTOs
- Execute APIs
- Provide a Bearer JWT token
- Test protected endpoints

Example:

```text
http://localhost:8080/swagger-ui/index.html
```

The exact Swagger exposure depends on the service/gateway configuration.

---

# Logging

The business services use structured application logging through SLF4J/Logback.

Useful events include:

- Customer registration attempts
- Login attempts
- Account creation
- Customer existence checks
- Deposit requests
- Withdrawal requests
- Transfer requests
- Transaction-history queries
- Ownership mismatches
- Invalid input
- Account-not-found conditions
- Inter-service communication failures
- Gateway authentication failures

## Security Logging Rules

The application must never log:

- Plain-text passwords
- Password hashes unnecessarily
- JWT tokens
- Database passwords
- JWT signing secrets

JWT tokens should never be printed to logs.

---

# Configuration and Environment Variables

Configuration is centralized through Config Server where applicable.

Sensitive values should be injected through environment variables.

Typical values include:

```text
DB_HOST
DB_PORT
DB_USERNAME
DB_PASSWORD
JWT_SECRET
EUREKA_SERVER_URL
CONFIG_SERVER_URL
```

Example:

```text
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_long_secure_secret
EUREKA_SERVER_URL=http://localhost:8761/eureka
CONFIG_SERVER_URL=http://localhost:8888
```

> Do not commit real passwords, JWT secrets or production credentials to GitHub.

---

# Prerequisites

Install:

- Java JDK 21
- Maven
- MySQL Server
- MySQL Workbench (optional)
- Postman (optional)
- IntelliJ IDEA / Eclipse / VS Code

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

# Database Setup

Unlike the Phase 4 monolith, Phase 5 uses separate databases.

Example:

```text
Customer Service
      │
      ▼
minibank_customer_db


Account Service
      │
      ▼
minibank_account_db
```

Example MySQL setup:

```sql
CREATE DATABASE minibank_customer_db;
CREATE DATABASE minibank_account_db;
```

The exact database names may be supplied through Config Server/environment variables.

Hibernate/JPA manages the tables according to the service-specific configuration.

Customer Service owns customer tables.

Account Service owns:

```text
accounts
transactions
```

No service should access the other's tables.

---

# Installation

## 1. Clone the Repository

```bash
git clone <your-repository-url>
```

## 2. Enter the Project

```bash
cd minibank-microservices
```

## 3. Configure MySQL

Create the separate databases:

```text
minibank_customer_db
minibank_account_db
```

## 4. Configure Environment Variables

Configure:

```text
DB_HOST
DB_PORT
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

and the required Config Server/Eureka settings.

## 5. Build Each Maven Project

Each service can be built independently:

```bash
cd config-server
mvn clean install
```

```bash
cd discovery-server
mvn clean install
```

```bash
cd api-gateway
mvn clean install
```

```bash
cd customer-service
mvn clean install
```

```bash
cd account-service
mvn clean install
```

---

# Running the Application

Each application is independently runnable.

Typical local ports:

```text
Config Server       → 8888
Eureka Server       → 8761
API Gateway         → 8080
Customer Service    → 8081
Account Service     → 8082
```

Run each project using:

```bash
mvn spring-boot:run
```

or run the corresponding Spring Boot main class from the IDE.

---

# Startup Order

A practical local startup order is:

```text
1. Config Server
       ↓
2. Eureka Discovery Server
       ↓
3. Customer Service
       ↓
4. Account Service
       ↓
5. API Gateway
```

The business services should remain independently startable.

Account Service should not fail during startup merely because Customer Service is temporarily unavailable.

The customer existence call occurs when an account is opened, not during Account Service boot.

---

# API Testing Flow

The recommended end-to-end flow is:

```text
1. Start Config Server
          ↓
2. Start Eureka
          ↓
3. Start Customer Service
          ↓
4. Start Account Service
          ↓
5. Start API Gateway
          ↓
6. Register Customer
          ↓
7. Login Customer
          ↓
8. Receive JWT
          ↓
9. Send JWT to Gateway
          ↓
10. Open Account
          ↓
11. Customer existence check
          ↓
12. Account created
          ↓
13. Check Balance
          ↓
14. Deposit
          ↓
15. Check Balance
          ↓
16. Withdraw
          ↓
17. Transfer
          ↓
18. View Transaction History
          ↓
19. Filter Transaction History
```

---

# Example API Flow

## Step 1 — Register Customer

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Nikhil Patidar",
  "email": "nikhil@example.com",
  "password": "password123"
}
```

---

## Step 2 — Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "nikhil@example.com",
  "password": "password123"
}
```

Example response:

```json
{
  "token": "<JWT_TOKEN>",
  "message": "Login successful"
}
```

Copy the JWT.

---

## Step 3 — Send JWT to Gateway

For protected APIs:

```http
Authorization: Bearer <JWT_TOKEN>
```

The Gateway authenticates/authorizes the request before routing it.

---

## Step 4 — Open Account

```http
POST http://localhost:8080/api/accounts/open
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "accountType": "SAVING"
}
```

Internally:

```text
Gateway
   ↓
account-service
   ↓
Read authenticated customerId
   ↓
OpenFeign
   ↓
customer-service
   ↓
GET /api/customers/{id}/exists
   ↓
Customer exists
   ↓
Create Account
```

---

## Step 5 — Deposit

```http
POST http://localhost:8080/api/deposit
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "accountNumber": "123456789012",
  "confirmAccountNumber": "123456789012",
  "amount": 5000
}
```

---

## Step 6 — Check Balance

```http
GET http://localhost:8080/api/accounts/123456789012
Authorization: Bearer <JWT_TOKEN>
```

---

## Step 7 — Withdraw

```http
POST http://localhost:8080/api/withdraw
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "accountNumber": "123456789012",
  "confirmAccountNumber": "123456789012",
  "amount": 1000
}
```

---

## Step 8 — Transfer

```http
POST http://localhost:8080/api/transfer
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "senderAccountNumber": "123456789012",
  "recipientAccountNumber": "987654321098",
  "confirmRecipientAccountNumber": "987654321098",
  "amount": 1000
}
```

---

## Step 9 — Transaction History

```http
GET http://localhost:8080/api/accounts/123456789012/history
Authorization: Bearer <JWT_TOKEN>
```

---

# Integration Testing

Phase 5 focuses on integration testing rather than the Phase 4 Mockito-only unit-test approach.

Integration tests verify that multiple application components work together.

Examples include:

- Controller + service
- Service + repository
- JPA + database
- Request validation
- Security filters
- API behavior
- Transaction persistence
- Account ownership
- Banking business rules
- Microservice endpoint behavior

The test suite can be executed using:

```bash
mvn test
```

Integration tests should verify externally observable application behavior rather than only testing individual methods in isolation.

---

# H2 Test Database

H2 is used as the test database so that integration tests do not depend on a developer's local MySQL installation.

The test architecture is:

```text
Integration Test
       │
       ▼
Spring Boot Application Context
       │
       ▼
Controller
       │
       ▼
Service
       │
       ▼
Repository / JPA
       │
       ▼
H2 Database
```

Production/runtime database:

```text
MySQL
```

Test database:

```text
H2
```

This separation provides:

- Faster tests
- Repeatable test execution
- No need for a running MySQL server during tests
- Isolated test data
- Safer automated test execution

H2 is a test database only and is not the production persistence technology.

---

# Integration Test Strategy

The integration test suite should cover important application flows such as:

## Customer

- Register customer
- Reject invalid registration
- Reject duplicate email
- Login with valid credentials
- Reject invalid credentials
- Verify password is not stored in plain text

## Account

- Open account
- Reject unsupported account type
- Reject duplicate account type for same customer
- Verify customer existence
- Reject nonexistent customer
- Check account balance
- List customer accounts
- Reject access to another customer's account

## Deposit

- Successful deposit
- Reject zero amount
- Reject negative amount
- Reject account-number confirmation mismatch
- Verify updated balance
- Verify transaction record

## Withdrawal

- Successful withdrawal
- Reject zero/negative amount
- Reject insufficient funds
- Verify balance
- Verify transaction record

## Transfer

- Successful transfer
- Reject self-transfer
- Reject insufficient funds
- Reject invalid amount
- Reject confirmation mismatch
- Verify sender balance
- Verify recipient balance
- Verify `TRANSFER_OUT`
- Verify `TRANSFER_IN`

## Transaction History

- Complete history
- Deposit history
- Withdrawal history
- Transfer-in history
- Transfer-out history
- Balance-based filtering
- Time-based filtering
- Status-based filtering
- Ownership validation

---

# Testing Commands

Run integration tests:

```bash
mvn test
```

Build without tests:

```bash
mvn clean package -DskipTests
```

Build and execute tests:

```bash
mvn clean verify
```

For an individual service:

```bash
cd customer-service
mvn test
```

```bash
cd account-service
mvn test
```

```bash
cd api-gateway
mvn test
```

---

# Failure Handling

Microservices introduce network failures that did not exist inside the monolith.

The project handles important failure scenarios explicitly.

## Customer Service Unavailable During Account Creation

```text
Client
  ↓
Gateway
  ↓
Account Service
  ↓
OpenFeign
  ↓
Customer Service unavailable
  ↓
Clear application error
```

The request should fail clearly rather than hanging indefinitely or exposing an unhandled stack trace.

Account Service must not depend on Customer Service being available during its own application startup.

---

# No Hardcoded Peer Addresses

The Java code must not contain:

```text
http://localhost:8081
http://localhost:8082
```

for peer-service communication.

Instead:

```text
Eureka Service ID
       ↓
customer-service
```

or:

```text
lb://customer-service
```

is used through the Spring Cloud infrastructure.

---

# Important Microservices Design Decisions

## 1. One Database Per Service

Customer Service owns its database.

Account Service owns its database.

No shared database is used.

---

## 2. No Cross-Service JPA Relationship

The old monolith could use:

```java
@ManyToOne
Customer customer;
```

That relationship is removed.

Account Service stores:

```text
customerId
```

instead.

---

## 3. Account and Transaction Stay Together

Account and Transaction are deliberately kept inside Account Service because a transfer changes both accounts and creates corresponding transaction entries.

This avoids a distributed transaction.

---

## 4. Customer Validation Uses a Network Call

Account creation requires the customer to exist.

Instead of a database join:

```text
Account Service
      ↓
OpenFeign
      ↓
Customer Service
```

This is a service-to-service boundary.

---

## 5. JWT Is Not Revalidated by Calling Customer Service

The Gateway verifies the JWT locally.

The system does not call Customer Service for every protected request.

This preserves the stateless nature of JWT authentication.

---

## 6. Authentication/Authorization at Gateway

The Gateway handles the authentication/authorization layer for incoming client requests.

The business services continue enforcing business-specific authorization such as account ownership.

This creates a layered security model:

```text
Gateway
  │
  ├── Is request authenticated?
  ├── Is request allowed through the API boundary?
  │
  ▼
Account Service
  │
  ├── Does account exist?
  ├── Does account belong to customer?
  ├── Is balance sufficient?
  └── Are banking rules satisfied?
```

---

## 7. DTOs at API Boundaries

DTOs are used at API boundaries.

This includes:

- Client-to-Gateway/API boundaries
- Controller request/response boundaries
- Customer existence service-to-service communication
- Feign request/response models

Entities should not be exposed directly as public API contracts.

---

## 8. Constructor Injection

Constructor injection is used throughout the services.

Dependencies should not be field-injected.

---

## 9. Centralized Configuration

Common and service-specific configuration is externalized through Config Server.

Sensitive configuration is supplied through secure environment variables.

---

## 10. H2 Is Test-Only

H2 is used for integration-test persistence.

MySQL remains the application database for normal runtime usage.

---

# Monolith vs Microservices

## Phase 4 — Monolith

```text
Single Spring Boot Application
          │
          ├── Customer
          ├── Account
          ├── Transaction
          ├── Authentication
          └── Database
```

Advantages:

- Simple deployment
- Simple debugging
- Local method calls
- One database transaction boundary

---

## Phase 5 — Microservices

```text
                  API Gateway
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
     Customer Service      Account Service
             │                   │
             ▼                   ▼
      Customer DB          Account DB
```

Additional infrastructure:

```text
Config Server
Eureka Discovery
OpenFeign
API Gateway
```

Advantages demonstrated by this project:

- Independent deployment
- Independent database ownership
- Service discovery
- Network-based communication
- Clear domain boundaries
- Independent scaling potential
- Failure isolation concepts

Additional complexity:

- Network failures
- Service discovery
- Centralized configuration
- Multiple deployments
- Distributed debugging
- API compatibility
- Operational monitoring

For MiniBank, the microservices version is primarily a learning exercise.

---

# Common Errors

## 1. 401 Unauthorized

Possible reasons:

- JWT missing
- JWT invalid
- JWT expired
- Invalid Authorization header
- Incorrect Bearer prefix
- Gateway security configuration rejected the request

Correct header:

```http
Authorization: Bearer <JWT_TOKEN>
```

---

## 2. 403 Forbidden

Possible reasons:

- Authenticated customer is trying to access another customer's account.
- Gateway authorization rejected the request.
- Account ownership validation failed.

---

## 3. 404 Not Found

Possible reasons:

- Incorrect API path
- Gateway route does not match
- Requested account does not exist
- Requested resource does not exist

A path that matches no Gateway route should return a clean 404.

---

## 4. Eureka Service Not Registered

Check:

```text
Eureka Server is running
Service application name is correct
Eureka client configuration is correct
Network connection is available
```

Open:

```text
http://localhost:8761
```

and verify registered services.

---

## 5. Config Server Error

Check:

```text
Config Server is running
Configuration repository/source is available
Service configuration name matches spring.application.name
CONFIG_SERVER_URL is correct
```

---

## 6. Customer Service Unavailable During Account Creation

This affects:

```http
POST /api/accounts/open
```

because Account Service must verify the customer through Customer Service.

The request should return a clear service-communication error rather than waiting indefinitely.

---

## 7. Database Connection Error

Check:

```text
DB_HOST
DB_PORT
DB_USERNAME
DB_PASSWORD
```

Also verify that the correct service database is being used.

Customer Service must connect only to its customer database.

Account Service must connect only to its account database.

---

## 8. OpenFeign Error

Check:

```text
Eureka Server
customer-service registration
Feign service name
Customer Service availability
Gateway-independent internal communication
```

The Feign client should use the logical service name, not a hardcoded host/port.

---

## 9. Account Service Fails Because Customer Service Is Down

This is a design bug if it occurs during startup.

Account Service should be able to boot independently.

The Customer Service dependency is required when creating an account, not when Account Service starts.

---

# Example End-to-End Architecture Flow

```text
                         CLIENT
                           │
                           ▼
                    API GATEWAY :8080
                           │
                    JWT Authentication
                    Authorization
                           │
             ┌─────────────┴─────────────┐
             │                           │
             ▼                           ▼
      CUSTOMER SERVICE             ACCOUNT SERVICE
           :8081                         :8082
             │                           │
             ▼                           ▼
       CUSTOMER DB                  ACCOUNT DB
                                         │
                              ┌──────────┴──────────┐
                              │                     │
                           Account              Transaction
                              │
                              │
                              │ OpenFeign
                              ▼
                       CUSTOMER SERVICE
                              │
                              ▼
                       Customer Exists?

EUREKA :8761
    ▲      ▲       ▲
    │      │       │
 Gateway  Customer Account

CONFIG SERVER :8888
    │
    ├── Gateway configuration
    ├── Customer configuration
    ├── Account configuration
    └── Discovery configuration
```

---

# Complete Account Creation Flow

```text
Client
  │
  │ POST /api/accounts/open
  │ Authorization: Bearer JWT
  ▼
API Gateway
  │
  │ Validate JWT / Authorization
  ▼
Account Service
  │
  ├── Read customerId
  │
  ├── Validate account type
  │
  ├── Check duplicate account type
  │
  ├── Call Customer Service
  │       │
  │       ▼
  │   GET /api/customers/{id}/exists
  │       │
  │       ▼
  │   Customer exists
  │
  ├── Create Account
  │
  └── Save Account
```

If the customer does not exist:

```text
Customer Service
      │
      ▼
exists = false
      │
      ▼
Account Service
      │
      ▼
AccountNotFoundException
      │
      ▼
Client receives error
```

---

# Complete Transfer Flow

```text
Client
  │
  ▼
API Gateway
  │
  ▼
Account Service
  │
  ├── Validate JWT context
  │
  ├── Validate sender ownership
  │
  ├── Validate recipient
  │
  ├── Validate confirmation
  │
  ├── Ensure sender != recipient
  │
  ├── Validate amount > 0
  │
  ├── Check sender balance
  │
  ├── Debit sender
  │
  ├── Credit recipient
  │
  ├── Create TRANSFER_OUT
  │
  ├── Create TRANSFER_IN
  │
  └── Commit local transaction
```

No distributed transaction is required because both accounts and transactions belong to Account Service.

---

# Security Rules

The project must preserve the following security requirements:

- JWT authentication is required for protected APIs.
- Authentication/authorization is handled at the Gateway.
- Account ownership is enforced inside Account Service.
- Passwords are BCrypt hashed.
- Plain-text passwords are never stored.
- Passwords are never logged.
- JWT secrets are externalized.
- JWT tokens are never logged.
- Database credentials are externalized.
- No peer-service host/port is hardcoded in Java code.
- Sensitive information is not returned by service-to-service APIs.
- Customer existence API returns only the minimum information required.

---

# Future Improvements

The current project demonstrates the required Phase 5 microservices pattern. Possible production-oriented improvements include:

- Replace shared JWT secret with asymmetric public/private key authentication.
- Add refresh-token support.
- Add centralized secret management.
- Add distributed tracing.
- Add correlation IDs.
- Add centralized log aggregation.
- Add metrics and monitoring.
- Add circuit breaker/resilience patterns.
- Add request timeouts and retry policies for Feign calls.
- Add rate limiting at the Gateway.
- Add API versioning.
- Add pagination for transaction history.
- Add transaction sorting.
- Add database migrations using Flyway or Liquibase.
- Add Docker Compose for the complete local environment.
- Add CI/CD using GitHub Actions.
- Add Testcontainers for MySQL integration testing.
- Add contract testing between services.
- Add health checks and readiness/liveness probes.
- Add centralized configuration encryption.
- Add load balancing and horizontal scaling.
- Add distributed tracing using OpenTelemetry.

---

# Version History

## Phase 5 — Microservices Version

### Added

- Microservices architecture
- Customer Service
- Account Service
- Eureka Discovery Server
- API Gateway
- Config Server
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Cloud OpenFeign
- Service-to-service customer existence check
- Independent customer database
- Independent account database
- `customerId` instead of cross-service JPA Customer relationship
- Gateway authentication and authorization
- JWT-based security
- Centralized configuration
- Integration testing
- H2 test database
- Independent Maven projects
- Gateway-based external API access

### Preserved

- Customer registration
- Login
- JWT issuing
- BCrypt password hashing
- Account creation
- SAVING and CURRENT account types
- 12-digit account numbers
- Balance checking
- Deposit
- Withdrawal
- Fund transfer
- Transaction history
- Transaction filtering
- Insufficient-fund validation
- Self-transfer prevention
- Confirmation-account validation
- Account ownership validation
- Jakarta Bean Validation
- Exception handling
- SLF4J logging
- Swagger/OpenAPI support

---

# Author

**Nikhil Patidar**

MiniBank — Phase 5 Microservices Banking Backend

### Technologies Used

- Java 21
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Security
- JWT
- BCrypt
- Spring Data JPA
- Hibernate
- MySQL
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Cloud Config
- Jakarta Validation
- Swagger/OpenAPI
- JUnit 5
- Spring Boot Test
- MockMvc
- H2
- Maven
- Lombok

---

# License

This project is developed as an educational banking backend project.

Unless otherwise specified, the project can be used and modified for learning and educational purposes.

---

# Acknowledgments

- Spring Boot documentation and community
- Spring Security documentation
- Spring Cloud documentation
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Cloud Config
- Spring Data JPA and Hibernate
- MySQL
- H2 Database
- Swagger / OpenAPI
- JUnit
- Maven
- Lombok
- Spring ecosystem and community

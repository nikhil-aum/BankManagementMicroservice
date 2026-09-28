# Bank Management System — Microservices Banking System

A secure, distributed banking backend built by transforming the Phase 4 Bank Management System monolith into an independently deployable microservices architecture.

The system keeps the original banking business rules unchanged while separating responsibilities across dedicated services. Service discovery, centralized configuration, API routing, Google OAuth 2.0 security, inter-service communication, independent databases, resilience patterns, correlation IDs, rate limiting, and integration testing are used to demonstrate a production-style microservices design.

> **Learning note:** For an application of this size, a monolith is completely reasonable in a real-world environment. This microservices version is intentionally built to learn service boundaries, service discovery, API gateway routing, centralized configuration, service-to-service communication, resilience, observability, and distributed-system concerns.

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
- [OAuth 2.0 Flow](#oauth-20-flow)
- [Service Discovery with Eureka](#service-discovery-with-eureka)
- [Inter-Service Communication](#inter-service-communication)
- [Resilience with Resilience4j](#resilience-with-resilience4j)
- [Circuit Breaker](#circuit-breaker)
- [Retry Mechanism](#retry-mechanism)
- [Rate Limiting](#rate-limiting)
- [Correlation ID and MDC](#correlation-id-and-mdc)
- [Centralized Configuration](#centralized-configuration)
- [Gateway Routing](#gateway-routing)
- [API Endpoints](#api-endpoints)
- [Business Rules](#business-rules)
- [Customer Identity Rules](#customer-identity-rules)
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

Bank Management System Phase 5 breaks the previous banking monolith into independently deployable services.

The system is organized as:

```text
                              ┌──────────────────────┐
                              │       Client         │
                              │ Postman / Browser    │
                              │      / Swagger       │
                              └──────────┬───────────┘
                                         │
                                         │ HTTP
                                         ▼
                              ┌──────────────────────┐
                              │     API Gateway      │
                              │       :8080          │
                              │ Routing + Security   │
                              │ OAuth 2.0 + Rate     │
                              │ Limiting              │
                              └──────────┬───────────┘
                                         │
                           ┌─────────────┴─────────────┐
                           │                           │
                           ▼                           ▼
                ┌────────────────────┐       ┌────────────────────┐
                │ Customer Service   │       │  Account Service  │
                │      :8081         │       │      :8082         │
                │                    │       │                    │
                │ Customer           │       │ Account            │
                │ Registration       │       │ Transaction        │
                │ Identity           │       │ Deposit            │
                │                    │       │ Withdrawal         │
                └─────────┬──────────┘       │ Transfer           │
                          │                  │ Balance            │
                          │                  │ History            │
                          │                  └─────────┬──────────┘
                          │                            │
                          ▼                            ▼
                ┌────────────────────┐       ┌────────────────────┐
                │ Customer Database  │       │ Account Database   │
                │       MySQL        │       │       MySQL        │
                └────────────────────┘       └────────────────────┘

                          ▲
                          │
                 ┌────────┴────────┐
                 │ Discovery Server│
                 │     Eureka      │
                 │      :8761      │
                 └─────────────────┘

                 ┌─────────────────────┐
                 │    Config Server    │
                 │       :8888         │
                 └─────────────────────┘

                 Resilience4j
                 ├── Circuit Breaker
                 ├── Retry
                 └── Rate Limiter

                 Observability
                 ├── Correlation ID
                 └── MDC Logging
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
                       ├── Account
                       ├── Transaction
                       │
                       ▼
                  Account DB

        account-service
              │
              │ OpenFeign
              ▼
        customer-service

        All services
              │
              ├── Eureka Discovery
              ├── Config Server
              ├── Correlation ID / MDC
              └── Resilience4j
```

---

# Services

The project contains the following independently runnable Maven applications:

```text
bank-management-microservices/
│
├── config-server/
├── discovery-server/
├── api-gateway/
├── customer-service/
└── account-service/
```

### Service Summary

| Service | Responsibility | Port |
|---|---|---:|
| `config-server` | Centralized application configuration | 8888 |
| `discovery-server` | Eureka service registry | 8761 |
| `api-gateway` | Routing, authentication, authorization and rate limiting | 8080 |
| `customer-service` | Customer domain and customer identity integration | 8081 |
| `account-service` | Account, transactions and banking operations | 8082 |

Each service is independently buildable and runnable.

---

# Project Structure

```text
bank-management-microservices
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
│       │   │       ├── rateLimit
│       │   │       ├── correlation
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
        │   │       ├── client
        │   │       └── resilience
        │   └── resources
        │       └── application.yml
        │
        └── test
            └── java
```

> The exact package names may vary according to the implementation. The important architectural rule is that each service keeps its own controller/service/repository/entity/dto/exception/config layers. Resilience and correlation components may be placed in dedicated packages or infrastructure/configuration classes.

---

# Service Responsibilities

## 1. Config Server

The Config Server provides centralized configuration for the microservices.

Configuration that can be centralized includes:

- Service names
- Eureka configuration
- Database configuration
- Google OAuth 2.0 configuration
- Gateway configuration
- Resilience4j configuration
- Rate limiting configuration
- Logging configuration
- Correlation ID configuration
- Application-specific properties

Secrets should still be supplied securely through environment variables or a secure configuration mechanism.

---

## 2. Discovery Server

The Discovery Server is implemented using Spring Cloud Netflix Eureka.

Responsibilities:

- Maintain the service registry
- Allow services to register themselves
- Allow services to discover other services
- Remove the need for hardcoded peer host/port addresses

The Discovery Server runs on:

```text
http://localhost:8761
```

---

## 3. API Gateway

The API Gateway is the single entry point for clients.

Responsibilities:

- Route incoming requests
- Authenticate requests using Google OAuth 2.0
- Apply authorization/security rules
- Apply rate limiting
- Generate or propagate correlation IDs
- Forward requests to the appropriate service
- Use Eureka service IDs for routing
- Return clean routing/security errors

Business logic such as balance validation, insufficient-fund checks and account ownership remains inside the appropriate business service.

---

## 4. Customer Service

Customer Service owns the customer domain.

Responsibilities:

- Customer registration
- Customer validation
- Duplicate email checking
- Password hashing where applicable to the existing customer domain
- Google OAuth 2.0 identity integration
- Customer existence verification
- Customer database ownership
- Correlation ID propagation and logging

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
- Customer-service communication through OpenFeign
- Resilience4j circuit breaker/retry handling for remote customer checks
- Correlation ID propagation and MDC logging

Account and Transaction remain together deliberately because a transfer changes two accounts and creates double-entry transaction records.

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
| Google OAuth 2.0 | Authentication |
| BCrypt | Password hashing where applicable |
| Spring Cloud Netflix Eureka | Service discovery |
| Spring Cloud Gateway | API Gateway |
| Spring Cloud OpenFeign | Service-to-service communication |
| Spring Cloud Config | Centralized configuration |
| Resilience4j | Circuit breaker, retry and rate limiting |
| Jakarta Bean Validation | Request validation |
| springdoc OpenAPI | API documentation |
| SLF4J / Logback | Application logging |
| MDC | Correlation ID propagation in logs |
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

## Customer Service

Customer Service owns the customer domain.

Responsibilities:

- Customer profile/domain data
- Customer identity mapping after Google OAuth 2.0 authentication
- Customer existence verification
- Customer database ownership
- Customer-related validation required by the banking domain

Authentication is delegated to Google through OAuth 2.0. The application does not maintain a local password-based login flow when Google OAuth 2.0 is configured as the authentication mechanism.

Customer Service does not own account or transaction tables.

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

The Gateway provides a single public API and acts as the external security boundary.

```text
Client
  │
  ▼
Gateway :8080
  │
  ├── Google OAuth 2.0
  ├── Authentication
  ├── Authorization
  ├── Rate Limiting
  ├── Correlation ID
  │
  ├── /api/customers/** → customer-service
  └── /api/accounts/**  → account-service
```

The Gateway is responsible for:

- Routing
- Integrating Google OAuth 2.0 login through Spring Security
- Authentication
- Authorization/security filtering
- Rate limiting
- Correlation ID creation/propagation
- Handling unauthenticated requests
- Forwarding authenticated requests
- Handling clean routing errors

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
Google OAuth 2.0 Login
   │
   ▼
Google Identity Provider
   │
   ▼
Authenticated Application User
   │
   ▼
API Gateway
   │
   ├── Authentication / Authorization
   ├── Rate Limiting
   └── Correlation ID
   │
   ▼
Target Microservice
```

Protected requests use the application's authenticated security context.

The Gateway performs the external request security check before forwarding protected requests.

---

# OAuth 2.0 Flow

```text
1. Customer starts Google login
          ↓
2. Gateway redirects to Google
          ↓
3. Google authenticates the customer
          ↓
4. Google redirects to the configured callback
          ↓
5. Spring Security establishes authenticated context
          ↓
6. Client accesses protected banking APIs
          ↓
7. Gateway authenticates/authorizes request
          ↓
8. Gateway forwards request
          ↓
9. Account/Customer Service processes request
```

Protected request:

```http
Authorization: Bearer <access-token>
```

The exact token/session handling depends on the configured Spring Security OAuth 2.0 flow.

---

# OAuth 2.0 Configuration

Google OAuth 2.0 client configuration is required for the login flow.

Typical configuration values are:

```text
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
```

The credentials should be injected through environment variables or a secure configuration mechanism.

Example:

```text
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

Do not commit real Google client secrets to GitHub.

### OAuth 2.0 Security Model

```text
Google
  │
  │ authenticates user
  ▼
OAuth 2.0 Authorization Code
  │
  ▼
Spring Security OAuth2 Client
  │
  ▼
Authenticated Application User
  │
  ▼
Protected Banking APIs
```

Google remains the identity provider. The application does not receive or store the user's Google password.

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

# Resilience with Resilience4j

Microservices communicate over a network, so failures such as timeouts, temporary service unavailability and connection errors can occur.

This project uses **Resilience4j** to demonstrate:

- Circuit Breaker
- Retry
- Rate Limiting

The resilience layer is primarily applied to remote/service-boundary operations where it is appropriate.

Architecture:

```text
Account Service
      │
      ▼
   OpenFeign
      │
      ▼
Resilience4j
  ┌───────────────┐
  │ CircuitBreaker│
  │ Retry         │
  └───────────────┘
      │
      ▼
Customer Service
```

The objective is to prevent temporary downstream failures from causing unnecessary cascading failures.

---

# Circuit Breaker

A Circuit Breaker monitors calls to a remote dependency.

For this project, the primary example is:

```text
account-service
      │
      ▼
customer-service
```

Conceptually:

```text
                 ┌──────────────────┐
                 │   CLOSED         │
                 │ Normal requests  │
                 └────────┬─────────┘
                          │
                    failures increase
                          │
                          ▼
                 ┌──────────────────┐
                 │     OPEN         │
                 │ Calls rejected   │
                 │ immediately      │
                 └────────┬─────────┘
                          │
                    wait duration
                          │
                          ▼
                 ┌──────────────────┐
                 │   HALF_OPEN      │
                 │ Test requests    │
                 └───────┬──────────┘
                         │
                ┌────────┴────────┐
                │                 │
             success            failure
                │                 │
                ▼                 ▼
             CLOSED              OPEN
```

## Why Circuit Breaker Is Used

Without a circuit breaker:

```text
Account Service
      │
      │ repeated calls
      ▼
Customer Service DOWN
      │
      ▼
timeouts / failures
      │
      ▼
threads remain busy
      │
      ▼
resource exhaustion
```

With a circuit breaker:

```text
Customer Service DOWN
        │
        ▼
Failure threshold reached
        │
        ▼
Circuit OPEN
        │
        ▼
Calls fail fast
        │
        ▼
Account Service remains responsive
```

The circuit breaker does not make the downstream service available. It prevents repeated calls from continuously consuming resources while the dependency is unavailable.

---

# Circuit Breaker Configuration Example

A typical Resilience4j configuration can be centralized:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      customerService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

Meaning, conceptually:

- Inspect a rolling window of calls.
- Start evaluating failures after the minimum number of calls.
- Open the circuit when the configured failure percentage is reached.
- Keep the circuit open for the configured wait duration.
- Allow a limited number of trial calls in HALF_OPEN state.

Exact values should be tuned according to the actual application's traffic and failure characteristics.

---

# Circuit Breaker Fallback

A fallback should return a controlled application-level response.

Example conceptual flow:

```text
Account Service
      │
      ▼
Customer existence request
      │
      ▼
Circuit Breaker
      │
      ├── CLOSED → call Customer Service
      │
      └── OPEN → fallback
                    │
                    ▼
             controlled error
```

For account creation, the fallback should **not** create an account when Customer Service cannot confirm customer existence.

A safe behavior is:

```text
Customer verification unavailable
          ↓
Do not create account
          ↓
Return clear service-unavailable response
```

This prevents invalid accounts from being created when the customer verification dependency cannot be reached.

---

# Retry Mechanism

Retry is used for temporary/transient failures.

Example:

```text
Account Service
      │
      ▼
Customer Service
      │
      └── temporary failure
              │
              ▼
           Retry #1
              │
              └── temporary failure
                      │
                      ▼
                   Retry #2
                      │
                      ▼
                 Final failure
```

Retry is useful for short-lived failures such as:

- Temporary network interruption
- Connection reset
- Temporary service unavailability

Retry should **not** be used blindly for every exception.

---

# Retry Configuration Example

A typical configuration can be:

```yaml
resilience4j:
  retry:
    instances:
      customerService:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
```

Conceptually:

```text
maxAttempts: 3
```

means the operation can be attempted up to three times in total, depending on the Resilience4j configuration and invocation model.

The retry policy should be limited to transient failures. Business exceptions such as:

```text
CustomerNotFound
InvalidRequest
ValidationException
```

should generally not be retried because repeating the same request will not fix the business condition.

---

# Retry + Circuit Breaker Relationship

Retry and Circuit Breaker solve different problems.

### Retry

Answers:

> "Can a temporary failure succeed if I try again?"

### Circuit Breaker

Answers:

> "Should I stop calling a dependency that is repeatedly failing?"

Combined flow:

```text
Request
  │
  ▼
Circuit Breaker
  │
  ▼
Retry
  │
  ▼
Customer Service
  │
  ├── Success ───────────────► Response
  │
  └── Temporary Failure
          │
          ▼
       Retry
          │
          └── failures continue
                    │
                    ▼
              Circuit statistics
                    │
                    ▼
             Circuit may OPEN
```

Retry should not be configured with large attempt counts or long delays because excessive retries can increase latency and amplify load during an outage.

---

# Resilience4j Ordering

The exact decorator ordering depends on the implementation.

A common conceptual arrangement is:

```text
CircuitBreaker
      ↓
Retry
      ↓
Feign Call
      ↓
Customer Service
```

The project should keep the resilience policy consistent across remote calls and document any implementation-specific ordering.

---

# Rate Limiting

Rate limiting controls how many requests a client can make during a defined time window.

In this project, rate limiting is applied at the API Gateway using Resilience4j or the configured Gateway rate-limiting mechanism.

Example configuration:

```text
20 requests
per
60 seconds
```

Conceptually:

```text
Client
  │
  ▼
API Gateway
  │
  ▼
Rate Limiter
  │
  ├── Request 1  → Allow
  ├── Request 2  → Allow
  ├── ...
  ├── Request 20 → Allow
  │
  └── Request 21 → Reject
```

After the configured refresh period, the available request capacity is refreshed according to the selected rate-limiter configuration.

---

# Rate Limiter Configuration Example

Example Resilience4j configuration:

```yaml
resilience4j:
  ratelimiter:
    instances:
      gatewayRateLimiter:
        limitForPeriod: 20
        limitRefreshPeriod: 60s
        timeoutDuration: 0s
```

This means:

```text
20 requests
within a 60-second refresh period
```

and:

```text
timeoutDuration: 0s
```

means a request does not wait for permission. If no permission is immediately available, it is rejected.

The exact behavior depends on how the limiter is integrated into the Gateway.

---

# Rate Limiting Flow

```text
                    Client
                      │
                      ▼
                 API Gateway
                      │
                Rate Limiter
                      │
             ┌────────┴────────┐
             │                 │
          Allowed            Limited
             │                 │
             ▼                 ▼
       Authentication       429 response
             │
             ▼
          Routing
             │
             ▼
       Microservice
```

Rate limiting helps protect the API boundary from excessive request volume and accidental request bursts.

A rate limiter does not replace authentication, authorization, or business validation.

---

# Rate Limit Response

When the configured limit is exceeded, the Gateway should return:

```http
429 Too Many Requests
```

Example response:

```json
{
  "status": 429,
  "message": "Too many requests. Please try again later."
}
```

The exact response structure depends on the Gateway's exception handling implementation.

---

# Rate Limiting Design Considerations

A production system may use different limits based on:

- User identity
- Client IP
- API key
- Route
- Tenant
- Authentication status

For this educational project, the Gateway can demonstrate a basic rate-limiting policy.

For distributed deployments with multiple Gateway instances, a local in-memory limiter has limitations because each instance maintains its own state. A distributed rate limiter backed by a shared store such as Redis can be considered for a future production-oriented implementation.

---

# Correlation ID and MDC

Microservices generate logs across multiple applications.

A single request may produce:

```text
API Gateway
     ↓
Account Service
     ↓
Customer Service
```

Without a correlation ID, identifying which logs belong to the same request becomes difficult.

This project uses a **Correlation ID** and **MDC (Mapped Diagnostic Context)**.

The correlation ID is used to connect logs belonging to the same request across services.

---

# Correlation ID Flow

```text
Client
  │
  │ X-Correlation-ID: abc-123
  ▼
API Gateway
  │
  ├── If ID exists → propagate it
  │
  └── If ID missing → generate a new ID
  │
  ▼
Account Service
  │
  │ X-Correlation-ID: abc-123
  ▼
Customer Service
  │
  │ X-Correlation-ID: abc-123
  ▼
Logs
```

Example:

```text
Correlation ID = 7f2a9b3c-...
```

The same ID should appear in logs generated while processing the same request.

---

# MDC Logging

MDC allows request-specific information to be stored in the logging context.

Conceptually:

```java
MDC.put("correlationId", correlationId);
```

The logging pattern can then include:

```text
correlationId
```

Example log format:

```text
2026-09-28 14:20:10 INFO
[correlationId=7f2a9b3c-12ab-45cd]
AccountService - Opening account
```

Another service can log:

```text
2026-09-28 14:20:10 INFO
[correlationId=7f2a9b3c-12ab-45cd]
CustomerService - Checking customer existence
```

The common correlation ID makes it possible to trace the request across services.

---

# Correlation ID Filter

A servlet filter or Gateway filter can:

1. Read `X-Correlation-ID`.
2. Generate one if it is missing.
3. Put the ID into MDC.
4. Add it to the response.
5. Propagate it to downstream service calls.
6. Clear MDC after request completion.

Conceptual logic:

```text
Request
  │
  ▼
Read X-Correlation-ID
  │
  ├── Present → use existing ID
  │
  └── Missing → generate UUID
  │
  ▼
MDC.put("correlationId", id)
  │
  ▼
Process request
  │
  ▼
Response header:
X-Correlation-ID: id
  │
  ▼
MDC.clear()
```

---

# Correlation ID Propagation with Feign

When Account Service calls Customer Service:

```text
Client
  │
  │ X-Correlation-ID: ABC123
  ▼
Gateway
  │
  │ ABC123
  ▼
Account Service
  │
  │ Feign RequestInterceptor
  │ X-Correlation-ID: ABC123
  ▼
Customer Service
```

A Feign `RequestInterceptor` can read the correlation ID from MDC and add it to outgoing requests.

Conceptually:

```java
String correlationId = MDC.get("correlationId");

if (correlationId != null) {
    template.header("X-Correlation-ID", correlationId);
}
```

This allows the same request ID to travel across the service boundary.

---

# Correlation ID Security

Correlation IDs are tracing identifiers, not authentication credentials.

Do not place:

- Passwords
- Access tokens
- Client secrets
- Personal secrets

inside the correlation ID.

The application should validate or normalize incoming correlation IDs according to the project's security requirements.

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
- Circuit breaker state changes
- Retry attempts where useful
- Rate-limit rejections
- Gateway authentication failures

## Example Distributed Logs

```text
[correlationId=ABC123] Gateway      - Request received
[correlationId=ABC123] AccountSvc   - Opening account
[correlationId=ABC123] AccountSvc   - Calling customer-service
[correlationId=ABC123] CustomerSvc  - Checking customer existence
[correlationId=ABC123] CustomerSvc  - Customer exists
[correlationId=ABC123] AccountSvc   - Account created
[correlationId=ABC123] Gateway      - Response returned
```

This creates a simple request trace without requiring a full distributed tracing system.

## Security Logging Rules

The application must never log:

- Google passwords / Google account credentials
- Google OAuth 2.0 client secrets
- OAuth 2.0 authorization codes or sensitive tokens
- Database passwords
- Access tokens
- Refresh tokens
- Sensitive personal information unnecessarily

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
Google OAuth 2.0 settings
Gateway routes
Resilience4j configuration
Rate limiter configuration
Logging configuration
Correlation ID configuration
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
| `/oauth2/**` and OAuth callback routes | API Gateway / Spring Security |
| `/api/customers/**` | customer-service |
| `/api/accounts/**` | account-service |

Conceptually:

```text
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

## Google OAuth 2.0 Login

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| GET | `/oauth2/authorization/google` | No | Start Google OAuth 2.0 login |
| GET | `/login/oauth2/code/google` | Google OAuth 2.0 callback | OAuth 2.0 authorization callback |

> The exact callback and post-login routes depend on the Spring Security OAuth 2.0 configuration.

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

The rules from the earlier Bank Management System phases remain enforced.

---

# Customer Identity Rules

- Authentication is performed through Google OAuth 2.0.
- Google account credentials are handled by Google.
- The application uses the authenticated identity/email to identify the customer according to the implementation.
- Customer identity uniqueness should be enforced according to the application's customer-domain implementation.
- Sensitive Google OAuth 2.0 credentials must not be logged or committed to source control.

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
Authorization: Bearer <access-token>
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
- Password where applicable
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

The Gateway handles security, rate-limiting and routing-related failures separately from banking business exceptions.

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
| `429 Too Many Requests` | Rate limit exceeded |
| `503 Service Unavailable` | Required downstream service is unavailable or circuit breaker is open |

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
- Authenticate through the configured OAuth 2.0 flow
- Test protected endpoints

Example:

```text
http://localhost:8080/swagger-ui/index.html
```

The exact Swagger exposure depends on the service/gateway configuration.

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
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
EUREKA_SERVER_URL
CONFIG_SERVER_URL
```

Example:

```text
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your_password
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-long-secure-secret
EUREKA_SERVER_URL=http://localhost:8761/eureka
CONFIG_SERVER_URL=http://localhost:8888
```

## Resilience4j Environment Configuration

Resilience4j values can also be externalized through Config Server.

Typical properties include:

```text
resilience4j.circuitbreaker.instances.customerService.*
resilience4j.retry.instances.customerService.*
resilience4j.ratelimiter.instances.gatewayRateLimiter.*
```

Do not hardcode production secrets or environment-specific credentials in source code.

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
bank-management_customer_db


Account Service
      │
      ▼
bank-management_account_db
```

Example MySQL setup:

```sql
CREATE DATABASE `bank-management_customer_db`;
CREATE DATABASE `bank-management_account_db`;
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

# Build and Run

## 1. Clone the Repository

```bash
git clone <your-repository-url>
```

## 2. Enter the Project

```bash
cd bank-management-microservices
```

## 3. Configure MySQL

Create the separate databases:

```text
bank-management_customer_db
bank-management_account_db
```

## 4. Configure Environment Variables

Configure:

```text
DB_HOST
DB_PORT
DB_USERNAME
DB_PASSWORD
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
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
6. Start Google OAuth 2.0 login
          ↓
7. Authenticate customer
          ↓
8. Open Account
          ↓
9. Customer existence check
          ↓
10. Account created
          ↓
11. Check Balance
          ↓
12. Deposit
          ↓
13. Check Balance
          ↓
14. Withdraw
          ↓
15. Transfer
          ↓
16. View Transaction History
          ↓
17. Filter Transaction History
```

---

# Resilience Testing Flow

The resilience features can be tested independently.

## Circuit Breaker Test

Temporarily stop Customer Service:

```text
Customer Service OFF
       ↓
Account Service
       ↓
Open Account
       ↓
Feign call fails
       ↓
Retry attempts
       ↓
Repeated failures
       ↓
Circuit opens
       ↓
Subsequent calls fail fast
```

Start Customer Service again and allow the circuit to move through HALF_OPEN before returning to normal CLOSED behavior.

## Retry Test

Introduce a temporary downstream failure.

Observe:

```text
Initial call
   ↓
Retry 1
   ↓
Retry 2
   ↓
Success / final failure
```

## Rate Limiter Test

With an example limit of:

```text
20 requests / 60 seconds
```

send more than 20 requests within the configured window.

Expected behavior:

```text
Allowed requests
      ↓
Limit reached
      ↓
429 Too Many Requests
```

## Correlation ID Test

Send:

```http
X-Correlation-ID: TEST-123
```

and inspect logs from:

```text
Gateway
Account Service
Customer Service
```

The same correlation ID should appear across the request flow.

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
- Resilience fallback behavior where applicable
- Rate-limiting behavior where applicable
- Correlation ID propagation where applicable

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
- Authenticate using the configured OAuth 2.0 flow
- Verify password is not stored in plain text where password storage is still part of the domain
- Verify customer existence endpoint

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

## Resilience

- Customer Service temporary failure
- Retry behavior
- Circuit breaker transition/fallback
- Controlled downstream failure response

## Rate Limiting

- Requests below configured limit are accepted
- Requests exceeding configured limit receive `429`
- Limiter refresh behavior

## Correlation ID

- Existing correlation ID is preserved
- Missing correlation ID is generated
- Response contains correlation ID
- Feign request propagates correlation ID
- MDC is populated during request processing
- MDC is cleared after request completion

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
Resilience4j
  ↓
OpenFeign
  ↓
Customer Service unavailable
  ↓
Retry
  ↓
Circuit Breaker / fallback
  ↓
Clear application error
```

The request should fail clearly rather than hanging indefinitely or exposing an unhandled stack trace.

Account Service must not depend on Customer Service being available during its own application startup.

---

# Resilience Failure Handling

A controlled failure path should look like:

```text
Remote Service Failure
        │
        ▼
      Retry
        │
   ┌────┴────┐
 Success   Failure
   │          │
   ▼          ▼
Response   Circuit statistics
              │
              ▼
        Circuit may OPEN
              │
              ▼
        Fallback response
```

For customer verification:

```text
Customer verification unavailable
          ↓
Do not create account
          ↓
Return controlled service-unavailable response
```

This prevents the system from making an unsafe assumption that a customer exists when the dependency cannot be verified.

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

## 5. Customer Verification Is Resilient

Because Customer Service is remote, Account Service uses:

```text
OpenFeign
    +
Retry
    +
Circuit Breaker
```

The fallback does not create an account when customer verification cannot be completed.

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
  ├── Is rate limit exceeded?
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

## 7. Rate Limiting at the Gateway

Rate limiting is applied at the external API boundary.

This helps protect downstream services from excessive request volume.

The Gateway rejects requests before routing them when the configured rate limit has been exceeded.

---

## 8. Correlation ID Across Services

The same correlation ID is propagated through:

```text
Gateway
   ↓
Account Service
   ↓
Customer Service
```

MDC stores the ID during request processing so logs can be correlated.

---

## 9. DTOs at API Boundaries

DTOs are used at API boundaries.

This includes:

- Client-to-Gateway/API boundaries
- Controller request/response boundaries
- Customer existence service-to-service communication
- Feign request/response models

Entities should not be exposed directly as public API contracts.

---

## 10. Constructor Injection

Constructor injection is used throughout the services.

Dependencies should not be field-injected.

---

## 11. Centralized Configuration

Common and service-specific configuration is externalized through Config Server.

Sensitive configuration is supplied through secure environment variables.

---

## 12. H2 Is Test-Only

H2 is used for integration-test persistence.

MySQL remains the application database for normal runtime usage.

---

## 13. Retry Is Used Only for Transient Failures

Retry should not repeat permanent business failures.

Examples that generally should not be retried:

```text
Invalid request
Customer not found
Invalid account type
Insufficient funds
Ownership violation
```

Retries are intended for temporary technical failures.

---

## 14. Circuit Breaker Prevents Repeated Downstream Calls

When a remote dependency repeatedly fails, the circuit breaker can move to OPEN state.

This allows the application to fail fast rather than continuously consuming resources on calls that are currently unlikely to succeed.

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
Resilience4j
Rate Limiting
Correlation ID
MDC Logging
API Gateway
```

The microservices version demonstrates:

- Independent deployment
- Independent database ownership
- Service discovery
- Network-based communication
- Clear domain boundaries
- Resilience patterns
- Rate limiting
- Distributed request correlation
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
- Resilience configuration
- Distributed observability

For Bank Management System, the microservices version is primarily a learning exercise.

---

# Common Errors

## 1. 401 Unauthorized

Possible reasons:

- Google OAuth 2.0 authentication missing
- Access token invalid
- Access token expired
- Invalid Authorization header
- Incorrect Bearer prefix
- Gateway security configuration rejected the request

Correct header:

```http
Authorization: Bearer <access-token>
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

## 4. 409 Conflict

Possible reasons:

- Duplicate customer email
- Duplicate account type for the same customer
- Other application-defined resource conflicts

---

## 5. 429 Too Many Requests

Possible reasons:

- Gateway rate limit has been exceeded.

Check:

```text
Rate limiter configuration
Request frequency
Configured refresh period
Client/IP/user rate-limiting key
```

---

## 6. 503 Service Unavailable

Possible reasons:

- Customer Service unavailable
- Circuit breaker is OPEN
- Required downstream dependency is unavailable
- Service communication failure

Check:

```text
Eureka registration
Customer Service availability
Feign configuration
Circuit breaker state
Retry configuration
```

---

## 7. Eureka Service Not Registered

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

## 8. Config Server Error

Check:

```text
Config Server is running
Configuration repository/source is available
Service configuration name matches spring.application.name
CONFIG_SERVER_URL is correct
```

---

## 9. Customer Service Unavailable During Account Creation

This affects:

```http
POST /api/accounts/open
```

because Account Service must verify the customer through Customer Service.

The request should use:

```text
OpenFeign
+
Retry
+
Circuit Breaker
```

and return a clear controlled error when verification cannot be completed.

---

## 10. Database Connection Error

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

## 11. OpenFeign Error

Check:

```text
Eureka Server
customer-service registration
Feign service name
Customer Service availability
Resilience4j configuration
```

The Feign client should use the logical service name, not a hardcoded host/port.

---

## 12. Account Service Fails Because Customer Service Is Down

This is a design bug if it occurs during startup.

Account Service should be able to boot independently.

The Customer Service dependency is required when creating an account, not when Account Service starts.

---

## 13. Correlation ID Missing From Logs

Check:

```text
Correlation ID filter
MDC.put()
MDC.clear()
Logging pattern
Feign RequestInterceptor
Gateway propagation
```

Verify that:

```text
X-Correlation-ID
```

is passed between services.

---

# Example End-to-End Architecture Flow

```text
                         CLIENT
                           │
                           ▼
                    API GATEWAY :8080
                           │
                    ┌──────┴───────┐
                    │              │
              OAuth 2.0       Rate Limiter
                    │              │
                    └──────┬───────┘
                           │
                    Correlation ID
                           │
             ┌─────────────┴─────────────┐
             │                           │
             ▼                           ▼
      CUSTOMER SERVICE             ACCOUNT SERVICE
           :8081                         :8082
             │                           │
             ▼                           │
       CUSTOMER DB                      │
                                         │
                              ┌──────────┴──────────┐
                              │                     │
                           Account              Transaction
                              │
                              │ OpenFeign
                              │ Retry
                              │ Circuit Breaker
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
    ├── Resilience4j configuration
    ├── Rate limiter configuration
    └── Logging configuration
```

---

# Complete Account Creation Flow

```text
Client
  │
  │ POST /api/accounts/open
  │ Authorization: Bearer <access-token>
  │ X-Correlation-ID: ABC123
  ▼
API Gateway
  │
  ├── Authenticate / authorize
  ├── Apply rate limit
  ├── Create/propagate correlation ID
  │
  ▼
Account Service
  │
  ├── Read customerId
  │
  ├── Validate account type
  │
  ├── Check duplicate account type
  │
  ├── Resilience4j Circuit Breaker
  │
  ├── Retry policy
  │
  └── OpenFeign
          │
          ▼
    Customer Service
          │
          ▼
GET /api/customers/{id}/exists
          │
          ▼
   Customer exists?
      ┌───┴────┐
     YES       NO
      │         │
      ▼         ▼
 Create      Controlled error
 Account
```

---

# Complete Transfer Flow

```text
Client
  │
  ▼
API Gateway
  │
  ├── Authentication
  ├── Rate Limiting
  └── Correlation ID
  │
  ▼
Account Service
  │
  ├── Validate authenticated customer
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

- Google OAuth 2.0 is used for user authentication.
- Protected banking APIs require an authenticated user.
- Authentication is integrated at the Gateway/security boundary.
- Account ownership is enforced inside Account Service.
- Google passwords are never stored by the application.
- Google OAuth 2.0 client secrets are externalized.
- OAuth authorization codes, client secrets, access tokens and sensitive credentials are never logged.
- Database credentials are externalized.
- No peer-service host/port is hardcoded in Java code.
- Sensitive information is not returned by service-to-service APIs.
- Customer existence APIs return only the minimum information required.
- Rate limiting is applied at the API boundary.
- Correlation IDs contain no sensitive credentials.

---

# Future Improvements

The current project demonstrates the required Phase 5 microservices pattern. Possible production-oriented improvements include:

- Replace shared OAuth 2.0 client-secret approaches with an appropriate asymmetric-token validation architecture where required.
- Add centralized secret management.
- Add distributed tracing with OpenTelemetry.
- Add W3C Trace Context support.
- Add centralized log aggregation.
- Add metrics and monitoring.
- Add Prometheus/Grafana monitoring.
- Add Redis-backed distributed rate limiting.
- Add circuit-breaker metrics and dashboards.
- Add more granular retry policies.
- Add request timeouts and retry policies for Feign calls.
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
- Add full distributed tracing and service dependency visualization.

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
- Google OAuth 2.0 authentication
- Spring Security OAuth2 Client
- Google login flow
- Centralized configuration
- Resilience4j Circuit Breaker
- Resilience4j Retry mechanism
- Resilience4j Rate Limiter
- API Gateway rate limiting
- Correlation ID generation/propagation
- MDC-based request logging
- Feign correlation ID propagation
- Integration testing
- H2 test database
- Independent Maven projects
- Gateway-based external API access

### Preserved

- Customer domain management
- Google-based user authentication
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

Bank Management System — Phase 5 Microservices Banking Backend

### Technologies Used

- Java 21
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Security
- Spring Security OAuth2 Client
- Google OAuth 2.0
- Spring Data JPA
- Hibernate
- MySQL
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Cloud Config
- Resilience4j
- Circuit Breaker
- Retry
- Rate Limiter
- Correlation ID
- MDC / SLF4J / Logback
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
- Resilience4j documentation and community
- Spring Data JPA and Hibernate
- MySQL
- H2 Database
- Swagger / OpenAPI
- JUnit
- Maven
- Lombok
- Spring ecosystem and community

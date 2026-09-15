package com.bankManagement.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("customer-service", r -> r
                        .path("/api/customers/**")
                        .uri("lb://CUSTOMER-SERVICE")
                )
                .route("customer-service-v3-docs", r -> r
                        .path("/v3/api-docs/customer-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/customer-service", "/v3/api-docs"))
                        .uri("lb://CUSTOMER-SERVICE")
                )
                .route("account-service", r -> r
                        .path(
                                "/api/accounts/**",
                                "/api/deposit/**",
                                "/api/withdraw/**",
                                "/api/transfer/**",
                                "/api/transactions/**"
                        )
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .route("account-service-v3-docs", r -> r
                        .path("/v3/api-docs/account-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/account-service", "/v3/api-docs"))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .build();
    }
}
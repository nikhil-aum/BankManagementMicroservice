package com.bankManagement.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${JWT_SECRET}")
    private String secret;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        logger.info("Incoming request for path: {}", path);

        if (isPublicEndpoint(path)) {
            logger.info("Public endpoint accessed: {}. Skipping JWT validation.", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isBlank()) {
            logger.warn("Authorization header is missing for path: {}", path);
            return unauthorized(exchange, "Authorization header is missing");
        }

        if (!authHeader.startsWith("Bearer ")) {
            logger.warn("Invalid Authorization header format for path: {}", path);
            return unauthorized(exchange, "Invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            logger.warn("JWT token is empty for path: {}", path);
            return unauthorized(exchange, "JWT token is missing");
        }

        try {
            Key signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object customerIdClaim = claims.get("customerId");
            if (customerIdClaim == null) {
                logger.error("JWT token missing customerId claim for path: {}", path);
                return unauthorized(exchange, "Token missing customerId claim");
            }
            String customerId = String.valueOf(customerIdClaim);
            String email = claims.getSubject();

            logger.info("JWT validated successfully. CustomerId: {}, Email: {}", customerId, email);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r
                            .header("X-Customer-Id", customerId)
                            .header("X-Customer-Email", email)
                    )
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired JWT token"
            );
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {

        logger.warn("Unauthorized access: {}", message);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        exchange.getResponse()
                .getHeaders()
                .add(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                );

        String responseBody = """
                {
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "%s"
                }
                """.formatted(message);

        byte[] bytes = responseBody.getBytes(
                StandardCharsets.UTF_8
        );

        var buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

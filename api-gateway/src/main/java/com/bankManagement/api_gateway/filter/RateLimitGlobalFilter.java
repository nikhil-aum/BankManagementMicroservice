package com.bankManagement.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RateLimitGlobalFilter
        implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;

    public RateLimitGlobalFilter(
            RateLimiter gatewayRateLimiter) {

        this.rateLimiter = gatewayRateLimiter;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        boolean permitted =
                rateLimiter.acquirePermission();

        if (!permitted) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.TOO_MANY_REQUESTS
                    );

            return exchange.getResponse()
                    .setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isExcludedPath(String path) {

        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/favicon.ico");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
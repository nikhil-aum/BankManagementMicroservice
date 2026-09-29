package com.bankManagement.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = UUID.randomUUID().toString();

        exchange.getAttributes().put(MDC_KEY, correlationId);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, correlationId))
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();

        mutated.getResponse().getHeaders().set(HEADER, correlationId);

        long start = System.nanoTime();
        withMdc(correlationId, () ->
                log.info("--> {} {}", request.getMethod(), request.getURI().getPath()));

        return chain.filter(mutated).doFinally(signal -> withMdc(correlationId, () ->
                log.info("<-- {} {} status={} {}ms",
                        request.getMethod(),
                        request.getURI().getPath(),
                        mutated.getResponse().getStatusCode(),
                        (System.nanoTime() - start) / 1_000_000)));
    }


    private static void withMdc(String id, Runnable logCall) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, id)) {
            logCall.run();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
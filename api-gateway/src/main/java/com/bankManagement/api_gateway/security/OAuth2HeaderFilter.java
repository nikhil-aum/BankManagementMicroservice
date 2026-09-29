package com.bankManagement.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OAuth2HeaderFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2HeaderFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(c -> c.getAuthentication() instanceof OAuth2AuthenticationToken)
                .map(c -> (OAuth2AuthenticationToken) c.getAuthentication())
                .flatMap(oauthToken -> {
                    OAuth2User user = oauthToken.getPrincipal();
                    String email = user.getAttribute("email");
                    String name = user.getAttribute("name");

                    String correlationId = exchange.getAttribute("correlationId");

                    try (MDC.MDCCloseable ignored =
                                 MDC.putCloseable("correlationId", correlationId != null ? correlationId : "")) {
                        logger.info("Relaying authenticated Google user: {}", email);
                    }

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r
                                    .header("X-Customer-Email", email != null ? email : "")
                                    .header("X-Customer-Name", name != null ? name : "")
                            )
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
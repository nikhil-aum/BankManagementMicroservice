package com.bankManagement.account_service.util;

import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetryEventLoggerConfig {

    private static final Logger logger = LoggerFactory.getLogger(RetryEventLoggerConfig.class);

    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerListeners() {
        retryRegistry.retry("customer-service").getEventPublisher()
                .onRetry(event -> logger.warn(
                        "Retry attempt #{} for customer-service call, last error: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().toString()));
    }
}
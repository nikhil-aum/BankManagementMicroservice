package com.bankManagement.account_service.filter;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFeignInterceptorTest {

    @InjectMocks
    private CorrelationIdFeignInterceptor interceptor;

    @Mock
    private RequestTemplate requestTemplate;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void apply_WhenCorrelationIdPresentInMDC_ShouldHeaderBeAdded() {

        String correlationId = "test-correlation-123";
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);


        interceptor.apply(requestTemplate);
        verify(requestTemplate).header(CorrelationIdFilter.HEADER, correlationId);
    }

    @Test
    void apply_WhenCorrelationIdMissingInMDC_ShouldNotAddHeader() {


        interceptor.apply(requestTemplate);


        verify(requestTemplate, never()).header(CorrelationIdFilter.HEADER, (String) null);
        verify(requestTemplate, never()).header(CorrelationIdFilter.HEADER, "");
    }
}
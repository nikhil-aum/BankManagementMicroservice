package com.bankManagement.account_service.filter;

import com.bankManagement.account_service.fallback.CustomerClientFallbackFactory;
import com.bankManagement.account_service.feign.CustomerClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerClientFallbackFactoryTest {

    private CustomerClientFallbackFactory fallbackFactory;

    @BeforeEach
    void setUp() {
        fallbackFactory = new CustomerClientFallbackFactory();
    }

    @Test
    void getCustomerByEmail_WhenCauseIsFeignException_ShouldRethrowFeignException() {

        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/customers",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        FeignException feignException = new FeignException.NotFound(
                "Customer not found",
                request,
                null,
                null
        );


        CustomerClient customerClient = fallbackFactory.create(feignException);


        assertThatThrownBy(() -> customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .isInstanceOf(FeignException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void getCustomerByEmail_WhenCauseIsGenericException_ShouldThrowRuntimeException() {

        Throwable genericCause = new RuntimeException("Connection timed out");

        CustomerClient customerClient = fallbackFactory.create(genericCause);


        assertThatThrownBy(() -> customerClient.getCustomerByEmail("nikhil@gmail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer Service unavailable: Connection timed out")
                .hasCause(genericCause);
    }
}
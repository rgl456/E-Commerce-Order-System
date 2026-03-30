package com.ragul.OrderService.config;

// When order-service calls inventory-service via Feign,
// this interceptor adds the X-Request-ID to the outgoing request.
// Now inventory-service logs will carry the same trace ID.
// Without this, correlation IDs die at service boundaries.

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignCorrelationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            template.header("X-Request-ID", requestId);
        }

        // Also propagate trace context from Micrometer Tracing
        // (handled automatically by feign-micrometer dependency)
    }
}

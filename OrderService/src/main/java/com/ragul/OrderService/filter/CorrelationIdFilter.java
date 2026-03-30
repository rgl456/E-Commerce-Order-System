package com.ragul.OrderService.filter;

// MDC = Mapped Diagnostic Context
// A thread-local map that Logback reads when formatting log lines
// Setting traceId in MDC means EVERY log.info/warn/error in this request
// automatically includes the trace ID — you don't have to pass it manually

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Request-ID";
    private static final String MDC_TRACE_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;

        // Use existing ID from gateway or generate new one
        String traceId = httpReq.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        // Put in MDC — now ALL log statements in this thread include requestId
        MDC.put(MDC_TRACE_KEY, traceId);

        try {
            chain.doFilter(req, res);
        } finally {
            // CRITICAL: Always clear MDC after the request
            // If using thread pools, MDC values leak into the next request
            // if not cleared. This is a very common bug in production.
            MDC.clear();
        }
    }
}
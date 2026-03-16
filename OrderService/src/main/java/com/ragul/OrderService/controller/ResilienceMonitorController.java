package com.ragul.OrderService.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/resilience")
@RequiredArgsConstructor
public class ResilienceMonitorController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @GetMapping("/status")
    public Map<String, Object> getResilienceStatus() {
        Map<String, Object> status = new HashMap<>();

        // Circuit breaker states
        Map<String, Object> circuitBreakers = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("state", cb.getState().name());

            CircuitBreaker.Metrics metrics = cb.getMetrics();
            cbInfo.put("failureRate", String.format("%.1f%%",
                    metrics.getFailureRate() < 0 ? 0 : metrics.getFailureRate()));
            cbInfo.put("slowCallRate", String.format("%.1f%%",
                    metrics.getSlowCallRate() < 0 ? 0 : metrics.getSlowCallRate()));
            cbInfo.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
            cbInfo.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            cbInfo.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            cbInfo.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());

            circuitBreakers.put(cb.getName(), cbInfo);
        });
        status.put("circuitBreakers", circuitBreakers);

        // Retry stats
        Map<String, Object> retries = new HashMap<>();
        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryInfo = new HashMap<>();
            retryInfo.put("numberOfSuccessfulCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryInfo.put("numberOfSuccessfulCallsWithRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryInfo.put("numberOfFailedCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryInfo.put("numberOfFailedCallsAfterRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
            retries.put(retry.getName(), retryInfo);
        });
        status.put("retries", retries);

        return status;
    }
}
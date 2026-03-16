package com.ragul.OrderService.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(){
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        registry.getEventPublisher()
                .onEntryAdded(event -> {
                    event.getAddedEntry().getEventPublisher()
                            .onStateTransition(e -> log.warn(
                                    "CIRCUIT BREAKER [{}] state changed: {} → {}",
                                    e.getCircuitBreakerName(),
                                    e.getStateTransition().getFromState(),
                                    e.getStateTransition().getToState()
                            ))
                            .onCallNotPermitted(e -> log.warn(
                                    "CIRCUIT BREAKER [{}] is OPEN — call rejected, using fallback",
                                    e.getCircuitBreakerName()
                            ))
                            .onError(e -> log.debug(
                                    "CIRCUIT BREAKER [{}] recorded failure: {} (failure rate: {}%)",
                                    e.getCircuitBreakerName(),
                                    e.getThrowable().getClass().getSimpleName(),
                                    String.format("%.1f", registry.circuitBreaker(e.getCircuitBreakerName())
                                            .getMetrics().getFailureRate())
                            ));
                });

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();

        registry.getEventPublisher()
                .onEntryAdded(event -> {
                    event.getAddedEntry().getEventPublisher()
                            .onRetry(e -> log.warn(
                                    "RETRY [{}] attempt #{} after: {}",
                                    e.getName(),
                                    e.getNumberOfRetryAttempts(),
                                    e.getLastThrowable().getMessage()
                            ))
                            .onSuccess(e -> {
                                if (e.getNumberOfRetryAttempts() > 0) {
                                    log.info(
                                            "RETRY [{}] succeeded after {} attempts",
                                            e.getName(),
                                            e.getNumberOfRetryAttempts()
                                    );
                                }
                            })
                            .onError(e -> log.error(
                                    "RETRY [{}] exhausted all {} attempts. Final error: {}",
                                    e.getName(),
                                    e.getNumberOfRetryAttempts(),
                                    e.getLastThrowable().getMessage()
                            ));
                });

        return registry;
    }

}

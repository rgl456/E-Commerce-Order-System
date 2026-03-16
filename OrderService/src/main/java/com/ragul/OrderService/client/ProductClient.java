package com.ragul.OrderService.client;

import com.ragul.OrderService.config.FeignConfig;
import com.ragul.OrderService.dto.ProductResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    @Bulkhead(name = "productService", type = Bulkhead.Type.SEMAPHORE)
    ProductResponse getProductById(@PathVariable("productId") Long productId);


    default ProductResponse getProductFallback(Long id, Throwable t){
        if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            // Circuit is OPEN — we're not even trying to call product-service
            // This is fast failure — no thread blocked waiting
        }
        // Return null — OrderService will throw a meaningful exception
        // We don't want to silently "succeed" with fake product data for orders
        return null;
    }



}

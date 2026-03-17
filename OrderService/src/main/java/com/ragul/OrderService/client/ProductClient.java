package com.ragul.OrderService.client;

import com.ragul.OrderService.config.FeignConfig;
import com.ragul.OrderService.dto.ProductResponse;
import com.ragul.OrderService.exception.ServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    @Bulkhead(name = "productService", type = Bulkhead.Type.SEMAPHORE)
    ProductResponse getProductById(@PathVariable("productId") Long productId);

//    Fallback method signature must follow:
//    Spring searches for:
//    same parameters + Throwable
//    same return type

    default ProductResponse getProductFallback(Long id, Throwable t){
        if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            // Circuit is OPEN — we're not even trying to call product-service
            // This is fast failure — no thread blocked waiting
            throw new ServiceUnavailableException(
                    "Product service temporarily unavailable (circuit open)"
            );
        }
        // Return null — OrderService will throw a meaningful exception
        // We don't want to silently "succeed" with fake product data for orders
        throw new ServiceUnavailableException(
                "Product service failed: " + t.getMessage()
        );
    }

}

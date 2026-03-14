package com.ragul.OrderService.client;

import com.ragul.OrderService.config.FeignConfig;
import com.ragul.OrderService.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    Optional<ProductResponse> getProductById(@PathVariable("productId") Long productId);

}

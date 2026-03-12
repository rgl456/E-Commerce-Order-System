package com.ragul.OrderService.client;

import com.ragul.OrderService.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product-service.url}")
    private String productServiceUrl;

    public Optional<ProductResponse> getProductById(Long productId){
        try{
            String url = productServiceUrl + "/api/v1/products/" + productId;
            ProductResponse response = restTemplate.getForObject(url, ProductResponse.class);
            return Optional.ofNullable(response);
        }
        catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found: {}", productId);
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.error("Product Service unavailable: {}", e.getMessage());
            throw new ServiceUnavailableException("Product Service is currently unavailable");
        }
    }



}

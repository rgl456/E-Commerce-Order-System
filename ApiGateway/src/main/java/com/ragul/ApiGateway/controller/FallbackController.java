package com.ragul.ApiGateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public ResponseEntity<Map<String, Object>> productFallback() {
        return buildFallback("product-service",
                "Product service is temporarily unavailable. Please try again shortly.");
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        return buildFallback("inventory-service",
                "Inventory service is temporarily unavailable. Please try again shortly.");
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return buildFallback("order-service",
                "Order service is temporarily unavailable. Please try again shortly.");
    }

    private ResponseEntity<Map<String, Object>> buildFallback(
            String service, String message) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "service", service,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString(),
                        "fallback", true
                ));
    }

}

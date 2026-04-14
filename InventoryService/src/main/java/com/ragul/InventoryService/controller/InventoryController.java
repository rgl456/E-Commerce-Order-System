package com.ragul.InventoryService.controller;

import com.ragul.InventoryService.dto.InventoryRequest;
import com.ragul.InventoryService.dto.InventoryResponse;
import com.ragul.InventoryService.dto.StockReservationRequest;
import com.ragul.InventoryService.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.addInventory(request));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(@Valid @RequestBody StockReservationRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        String callerClientId = jwt.getClaimAsString("azp");

        if(!callerClientId.equals("order-service-client")){
            log.warn("UNAUTHORIZED reserve attempt from: {}", callerClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseReservation(@Valid @RequestBody StockReservationRequest request,
                                                                @AuthenticationPrincipal Jwt jwt) {
        String callerClientId = jwt.getClaimAsString("azp");

        if(!callerClientId.equals("order-service-client")){
            log.warn("UNAUTHORIZED reserve attempt from: {}", callerClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(inventoryService.releaseReservation(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<InventoryResponse> confirmDeduction(@Valid @RequestBody StockReservationRequest request,
                                                              @AuthenticationPrincipal Jwt jwt) {
        String callerClientId = jwt.getClaimAsString("azp");

        if(!callerClientId.equals("order-service-client")){
            log.warn("UNAUTHORIZED reserve attempt from: {}", callerClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(inventoryService.confirmDeduction(request));
    }

}

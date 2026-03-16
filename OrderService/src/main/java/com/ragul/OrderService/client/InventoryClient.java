package com.ragul.OrderService.client;

import com.ragul.OrderService.config.FeignConfig;
import com.ragul.OrderService.dto.StockReservationRequest;
import com.ragul.OrderService.exception.InventoryServiceException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", configuration = FeignConfig.class)
public interface InventoryClient {

    @PostMapping("/api/v1/inventory/reserve")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveStockFallback")
    @Bulkhead(name = "inventory-service", type = Bulkhead.Type.SEMAPHORE)
    void stockReserve(@RequestBody StockReservationRequest request);

    @PostMapping("/api/v1/inventory/release")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseReservationFallback")
    @Retry(name = "inventoryService")
    void releaseReservation(@RequestBody StockReservationRequest request);

    default void reserveStockFallback(
            StockReservationRequest request, Throwable t) {
        // Can't reserve stock — this is a hard failure for order creation
        // We throw here because the order CANNOT be created without this step
        throw new InventoryServiceException(
                "Inventory service unavailable — cannot reserve stock: " + t.getMessage()
        );
    }

    default void releaseReservationFallback(
            StockReservationRequest request, Throwable t) {
        // Release failure is logged but not thrown — the user's experience
        // should not be degraded by a release failure during cancellation.
        // This goes to a reconciliation queue in production.
        throw new InventoryServiceException(
                "CRITICAL: Could not release reservation for order: "
                        + request.getOrderId()
                        + " — requires manual reconciliation"
        );
    }

}

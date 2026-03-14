package com.ragul.OrderService.client;

import com.ragul.OrderService.config.FeignConfig;
import com.ragul.OrderService.dto.StockReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", configuration = FeignConfig.class)
public interface InventoryClient {

    @PostMapping("/api/v1/inventory/reserve")
    void stockReserve(@RequestBody StockReservationRequest request);

    @PostMapping("/api/v1/inventory/release")
    void releaseReservation(@RequestBody StockReservationRequest request);

}

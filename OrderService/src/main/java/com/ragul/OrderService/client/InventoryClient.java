package com.ragul.OrderService.client;

import com.ragul.OrderService.dto.StockReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory-service.url}")
    private String inventoryServiceUrl;

    public void stockReserve(StockReservationRequest request){
        try{
            String url = inventoryServiceUrl + "/api/v1/inventory/reserve";
            restTemplate.postForObject(url, request, Object.class);
        }
        catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new InsufficientStockException(
                    "Insufficient stock for product: " + request.getProductId()
            );
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Inventory Service is currently unavailable");
        }
    }

    public void releaseReservation(StockReservationRequest request) {
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/release";
            restTemplate.postForObject(url, request, Object.class);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to release reservation for product {}. " +
                            "Manual reconciliation required. OrderId: {}",
                    request.getProductId(), request.getOrderId(), e);
        }
    }


}

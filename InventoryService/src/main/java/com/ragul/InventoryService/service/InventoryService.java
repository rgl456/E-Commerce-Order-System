package com.ragul.InventoryService.service;

import com.ragul.InventoryService.dto.InventoryRequest;
import com.ragul.InventoryService.dto.InventoryResponse;
import com.ragul.InventoryService.dto.StockReservationRequest;
import com.ragul.InventoryService.exception.InsufficientStockException;
import com.ragul.InventoryService.exception.InventoryAlreadyExistsException;
import com.ragul.InventoryService.exception.InventoryNotFoundException;
import com.ragul.InventoryService.model.Inventory;
import com.ragul.InventoryService.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ragul.InventoryService.mapper.InventoryMapper.mapToResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryResponse addInventory(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryAlreadyExistsException(
                    "Inventory already exists for product: " + request.productId()
            );
        }

        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .totalQuantity(request.totalQuantity())
                .reservedQuantity(0)
                .lowStockThreshold(request.lowStackThreshold())
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product: " + productId
                ));

        return mapToResponse(inventory);
    }

    @Transactional
    public InventoryResponse reserveStock(StockReservationRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product: " + request.getProductId()
                ));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for product %d. Requested: %d, Available: %d",
                    request.getProductId(),
                    request.getQuantity(),
                    inventory.getAvailableQuantity()
            ));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());

        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

    // if payment or order cancelled we can update the quantity and reserved quantity
    @Transactional
    public InventoryResponse releaseReservation(StockReservationRequest request) {
        Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product: " + request.getProductId()
                ));

        int newReserved = inventory.getReservedQuantity() - request.getQuantity();
        inventory.setReservedQuantity(Math.max(0, newReserved));

        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

    // Confirm deduction — called when order is CONFIRMED/SHIPPED
    @Transactional
    public InventoryResponse confirmDeduction(StockReservationRequest request) {
        Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product: " + request.getProductId()
                ));

        inventory.setTotalQuantity(inventory.getTotalQuantity() - request.getQuantity());

        int newReserved = inventory.getReservedQuantity() - request.getQuantity();
        inventory.setReservedQuantity(Math.max(0, newReserved));

        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

}

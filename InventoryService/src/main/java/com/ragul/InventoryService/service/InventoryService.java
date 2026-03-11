package com.ragul.InventoryService.service;

import com.ragul.InventoryService.dto.InventoryRequest;
import com.ragul.InventoryService.dto.InventoryResponse;
import com.ragul.InventoryService.model.Inventory;
import com.ragul.InventoryService.repository.InventoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
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


}

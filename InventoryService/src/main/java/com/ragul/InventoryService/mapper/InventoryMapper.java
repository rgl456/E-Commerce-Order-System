package com.ragul.InventoryService.mapper;

import com.ragul.InventoryService.dto.InventoryResponse;
import com.ragul.InventoryService.model.Inventory;

public class InventoryMapper {

    public static InventoryResponse mapToResponse(Inventory inventory){
        InventoryResponse response = InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .totalQuantity(inventory.getTotalQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .lowStock(inventory.getLowStockThreshold() >= inventory.getAvailableQuantity())
                .inStock(inventory.getAvailableQuantity() > 0)
                .build();

        return response;
    }

}

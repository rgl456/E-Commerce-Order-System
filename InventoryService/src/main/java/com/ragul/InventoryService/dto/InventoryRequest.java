package com.ragul.InventoryService.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer totalQuantity,

        @Min(value = 1, message = "Low stock threshold must be at least 1")
        Integer lowStackThreshold

) {
}

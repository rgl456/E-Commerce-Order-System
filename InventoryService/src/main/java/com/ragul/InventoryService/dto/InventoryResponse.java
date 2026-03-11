package com.ragul.InventoryService.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private Long id;
    private Long productId;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private boolean lowStock;
    private boolean inStock;

}
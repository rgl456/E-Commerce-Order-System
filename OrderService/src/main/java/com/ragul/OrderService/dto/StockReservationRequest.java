package com.ragul.OrderService.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRequest {
    private Long productId;
    private Integer quantity;
    private String orderId;
}
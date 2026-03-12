package com.ragul.OrderService.dto;

import com.ragul.OrderService.model.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String failureReason;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}

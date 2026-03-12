package com.ragul.OrderService.mapper;

import com.ragul.OrderService.dto.OrderItemResponse;
import com.ragul.OrderService.dto.OrderResponse;
import com.ragul.OrderService.model.Order;
import com.ragul.OrderService.model.OrderItem;

import java.util.ArrayList;
import java.util.List;


public class OrderMapper {

    public static OrderResponse mapToResponse(Order order){

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for(OrderItem item : order.getItems()) {
            OrderItemResponse itemResponse = OrderItemResponse.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
            itemResponses.add(itemResponse);
        }

        OrderResponse response = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .failureReason(order.getFailureReason())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();

        return response;
    }
}

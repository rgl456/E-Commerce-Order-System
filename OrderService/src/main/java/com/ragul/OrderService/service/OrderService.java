package com.ragul.OrderService.service;

import com.ragul.OrderService.client.InventoryClient;
import com.ragul.OrderService.client.ProductClient;
import com.ragul.OrderService.dto.*;
import com.ragul.OrderService.exception.*;
import com.ragul.OrderService.mapper.OrderMapper;
import com.ragul.OrderService.model.Order;
import com.ragul.OrderService.model.OrderItem;
import com.ragul.OrderService.model.OrderStatus;
import com.ragul.OrderService.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.ragul.OrderService.mapper.OrderMapper.mapToResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<ProductResponse> verifiedProducts = new ArrayList<>();

        for(OrderItemRequest itemRequest : request.items()){
            ProductResponse product = productClient.getProductById(itemRequest.productId())
                    .orElseThrow(() -> new OrderCreationException(
                            "Product not found: " + itemRequest.productId()
                    ));
            if(!"ACTIVE".equals((product.getStatus()))){
                throw new OrderCreationException(
                        "Product is not available: " + itemRequest.productId()
                );
            }

            verifiedProducts.add(product);
        }

        List<StockReservationRequest> reservationsMade = new ArrayList<>();

        try{
            for(int i=0;i<request.items().size();i++){
                OrderItemRequest orderItemRequest = request.items().get(i);
                ProductResponse product = verifiedProducts.get(i);

                StockReservationRequest reservationRequest = StockReservationRequest.builder()
                        .orderId(order.getOrderNumber() != null ?
                                order.getOrderNumber() :
                                "ORD-"+request.customerId()+new Random().nextInt(1000))
                        .productId(orderItemRequest.productId())
                        .quantity(orderItemRequest.quantity())
                        .build();

                inventoryClient.stockReserve(reservationRequest);
                reservationsMade.add(reservationRequest);

                BigDecimal subtotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(orderItemRequest.quantity()));

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .productId(product.getId())
                        .productName(product.getName())
                        .subtotal(subtotal)
                        .unitPrice(product.getPrice())
                        .quantity(orderItemRequest.quantity())
                        .build();

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(subtotal);
            }
        }
        catch (InsufficientStockException | ServiceUnavailableException e){

            for (StockReservationRequest made : reservationsMade) {
                inventoryClient.releaseReservation(made);
            }

            throw new OrderCreationException("Order failed: " + e.getMessage());
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }


    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        List<Order> orders = orderRepository.findAllByCustomerId(customerId);
        return orders.stream().map(OrderMapper::mapToResponse).toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancellationException(
                    "Cannot cancel order in status: " + order.getStatus()
            );
        }

        for (OrderItem item : order.getItems()) {
            inventoryClient.releaseReservation(StockReservationRequest.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .orderId(order.getOrderNumber())
                    .build());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return mapToResponse(orderRepository.save(order));
    }

}

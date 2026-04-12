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
import com.ragul.OrderService.security.UserContext;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.*;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ragul.OrderService.mapper.OrderMapper.mapToResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final UserContext userContext;


    @Transactional
    @RateLimiter(name = "order-creation", fallbackMethod = "rateLimitFallback")
    @Observed(name = "order.creation",
            contextualName = "creating-order",
            lowCardinalityKeyValues = {"service", "order-service"})
    public OrderResponse createOrder(OrderRequest request) {

        Long customerId = request.customerId();

        if(userContext.isCustomer() && !userContext.isAdmin()){
            log.info("Customer {} creating order for themselves",
                    userContext.getUserId());
            customerId = extractCustomerId();
        }

        log.info("Creating order for customer {}", customerId);

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID())
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<ProductResponse> verifiedProducts = new ArrayList<>();

        for(OrderItemRequest itemRequest : request.items()){
            ProductResponse product = productClient.getProductById(itemRequest.productId());

            if (!"ACTIVE".equals(product.getStatus())) {
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
                                "ORD-" + UUID.randomUUID())
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
                try {
                    inventoryClient.releaseReservation(made);
                } catch (Exception releaseEx) {
                    log.error("CRITICAL: Failed to release reservation for product {}",
                            made.getProductId(), releaseEx);
                }
            }

            throw new OrderCreationException("Order failed: " + e.getMessage());
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(order);

        log.info("Creating order for customer {}", customerId);

        return mapToResponse(savedOrder);
    }

    public OrderResponse rateLimitFallback(OrderRequest request, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for order creation. Customer: {}",
                request.customerId());
        throw new RateLimitExceededException(
                "Too many order requests. Please wait a moment and try again."
        );
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
            try {
                inventoryClient.releaseReservation(StockReservationRequest.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .orderId(order.getOrderNumber())
                        .build());
            } catch (Exception e) {
                log.error("Failed to release reservation during cancel for product {}",
                        item.getProductId(), e);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::mapToResponse)
                .toList();
    }

    public Long extractCustomerId() {
        String userId = userContext.getUserId();
        if (userId == null) return 0L;
        // user-service that maps Keycloak UUIDs to internal customer IDs
        return (long) Math.abs(userId.hashCode() % 1000);
    }

}

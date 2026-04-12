package com.ragul.OrderService.controller;

import com.ragul.OrderService.dto.OrderRequest;
import com.ragul.OrderService.dto.OrderResponse;
import com.ragul.OrderService.security.UserContext;
import com.ragul.OrderService.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserContext userContext;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {

        OrderResponse response = orderService.getOrderById(id);
        if(userContext.isAdmin()){
            return ResponseEntity.ok(response);
        }

        if(!response.getCustomerId().equals(orderService.extractCustomerId())){
            log.warn("Customer attempted to access order {} belonging to customer {}",
                     id, response.getCustomerId());
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        if(userContext.isAdmin()){
            return ResponseEntity.ok(response);
        }

        if(!response.getCustomerId().equals(orderService.extractCustomerId())){
            log.warn("Customer attempted to access order number {} belonging to customer {}",
                    orderNumber, response.getCustomerId());
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<OrderResponse>> getByCustomer(@PathVariable Long customerId) {

        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);
        if(userContext.isAdmin()){
            ResponseEntity.ok(orders);
        }

        if(!userContext.isAdmin() && userContext.isCustomer() && customerId.equals(orderService.extractCustomerId())){
            return ResponseEntity.ok(orders);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);

        if (!userContext.isAdmin() && !order.getCustomerId().equals(orderService.extractCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

}

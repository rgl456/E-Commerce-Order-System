package com.ragul.OrderService.model;

public enum OrderStatus {
    PENDING,        // order just created
    CONFIRMED,      // stock reserved, payment OK
    PROCESSING,     // being prepared
    SHIPPED,        // on its way
    DELIVERED,      // completed
    CANCELLED,      // cancelled after confirmation
    FAILED          // failed during creation
}

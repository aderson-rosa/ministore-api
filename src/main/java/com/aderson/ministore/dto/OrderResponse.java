package com.aderson.ministore.dto;

import com.aderson.ministore.domain.order.Order;
import com.aderson.ministore.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Instant createdAt,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(order.getId(), order.getCreatedAt(), order.getStatus(), order.getTotal(), items);
    }
}

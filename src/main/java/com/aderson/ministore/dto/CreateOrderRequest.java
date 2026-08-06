package com.aderson.ministore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "O pedido deve ter ao menos um item") @Valid List<OrderItemRequest> items
) {
}

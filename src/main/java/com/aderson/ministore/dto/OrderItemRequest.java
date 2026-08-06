package com.aderson.ministore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long productId,
        @Min(value = 1, message = "A quantidade deve ser pelo menos 1") int quantity
) {
}

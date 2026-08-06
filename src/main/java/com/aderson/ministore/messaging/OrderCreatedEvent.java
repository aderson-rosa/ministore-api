package com.aderson.ministore.messaging;

import java.math.BigDecimal;

/**
 * Evento publicado no RabbitMQ quando um pedido e criado.
 */
public record OrderCreatedEvent(
        Long orderId,
        BigDecimal total,
        int itemCount,
        String status
) {
}

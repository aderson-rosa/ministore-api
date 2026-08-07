package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de pedido no RabbitMQ.
 * Propaga a falha (AmqpException) de proposito: quem chama e o OutboxPublisher,
 * que, em caso de erro, mantem o evento PENDING para reprocessar depois.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                event);
        log.info("Evento '{}' publicado para o pedido {}",
                RabbitConfig.ORDER_CREATED_ROUTING_KEY, event.orderId());
    }
}

package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de pedido no RabbitMQ.
 * A publicacao e resiliente: se o broker estiver indisponivel, o pedido nao e perdido
 * (o evento apenas nao e enviado e a falha e logada).
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                    event);
            log.info("Evento '{}' publicado para o pedido {}",
                    RabbitConfig.ORDER_CREATED_ROUTING_KEY, event.orderId());
        } catch (AmqpException ex) {
            log.warn("Falha ao publicar evento do pedido {} no RabbitMQ: {}",
                    event.orderId(), ex.getMessage());
        }
    }
}

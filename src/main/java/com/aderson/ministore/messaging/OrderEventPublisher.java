package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import com.aderson.ministore.observability.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de pedido no RabbitMQ.
 * Propaga o correlation id (do MDC) para o header da mensagem, para o rastreamento
 * continuar no consumer. Propaga a falha (AmqpException) de proposito: quem chama
 * e o OutboxPublisher, que mantem o evento PENDING para reprocessar em caso de erro.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        final String correlationId = MDC.get(Correlation.MDC_KEY);

        MessagePostProcessor withCorrelation = message -> {
            if (correlationId != null) {
                message.getMessageProperties().setHeader(Correlation.HEADER, correlationId);
            }
            return message;
        };

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                event,
                withCorrelation);

        log.info("Evento '{}' publicado para o pedido {}",
                RabbitConfig.ORDER_CREATED_ROUTING_KEY, event.orderId());
    }
}

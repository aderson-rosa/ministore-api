package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventPublisher publisher;

    @Test
    void publishOrderCreated_enviaParaExchangeERoutingKeyCorretos() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, new BigDecimal("100.00"), 2, "CREATED");

        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ORDER_CREATED_ROUTING_KEY),
                eq(event));
    }

    @Test
    void publishOrderCreated_quandoBrokerFalha_propagaExcecao() {
        // Propaga a falha de proposito, para que o OutboxPublisher mantenha o evento
        // pendente e o reprocesse depois (garantia de entrega).
        OrderCreatedEvent event = new OrderCreatedEvent(2L, new BigDecimal("50.00"), 1, "CREATED");
        doThrow(new AmqpException("broker indisponivel"))
                .when(rabbitTemplate).convertAndSend(
                        eq(RabbitConfig.EXCHANGE),
                        eq(RabbitConfig.ORDER_CREATED_ROUTING_KEY),
                        eq(event));

        assertThatThrownBy(() -> publisher.publishOrderCreated(event))
                .isInstanceOf(AmqpException.class);
    }
}

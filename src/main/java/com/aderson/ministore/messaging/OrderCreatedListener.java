package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos de pedido criado a partir da fila do RabbitMQ.
 * Em um sistema real, aqui entrariam acoes como notificar o cliente,
 * dar baixa em um sistema de faturamento ou disparar a separacao no estoque.
 */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Pedido recebido via RabbitMQ -> id={}, total={}, itens={}, status={}",
                event.orderId(), event.total(), event.itemCount(), event.status());
    }
}

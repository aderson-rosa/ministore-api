package com.aderson.ministore.messaging;

import com.aderson.ministore.config.RabbitConfig;
import com.aderson.ministore.observability.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos de pedido criado a partir da fila do RabbitMQ.
 * Extrai o correlation id do header da mensagem para o MDC, mantendo o
 * rastreamento do mesmo fluxo ponta a ponta (produtor -> broker -> consumer).
 */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event,
                               @Header(name = Correlation.HEADER, required = false) String correlationId) {
        if (correlationId != null) {
            MDC.put(Correlation.MDC_KEY, correlationId);
        }
        try {
            log.info("Pedido recebido via RabbitMQ -> id={}, total={}, itens={}, status={}",
                    event.orderId(), event.total(), event.itemCount(), event.status());
        } finally {
            MDC.remove(Correlation.MDC_KEY);
        }
    }
}

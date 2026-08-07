package com.aderson.ministore.outbox;

import com.aderson.ministore.messaging.OrderCreatedEvent;
import com.aderson.ministore.messaging.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relay do padrao Outbox: periodicamente le os eventos pendentes na tabela de
 * outbox e os publica no RabbitMQ. Se a publicacao falhar (ex.: broker fora),
 * o evento permanece PENDING e sera reprocessado no proximo ciclo, garantindo
 * a entrega (at-least-once) sem perder eventos.
 */
@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           OrderEventPublisher orderEventPublisher,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publish.delay-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            try {
                OrderCreatedEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
                orderEventPublisher.publishOrderCreated(payload);
                event.markSent();
                log.info("Outbox: evento '{}' do pedido {} publicado", event.getType(), event.getAggregateId());
            } catch (Exception ex) {
                // Mantem PENDING e sera reprocessado no proximo ciclo (nao perde o evento).
                log.warn("Outbox: falha ao publicar evento id={}, sera reprocessado. Causa: {}",
                        event.getId(), ex.getMessage());
            }
        }
    }
}

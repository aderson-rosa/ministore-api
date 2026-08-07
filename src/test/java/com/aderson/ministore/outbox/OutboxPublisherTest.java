package com.aderson.ministore.outbox;

import com.aderson.ministore.messaging.OrderCreatedEvent;
import com.aderson.ministore.messaging.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, orderEventPublisher, objectMapper);
    }

    private OutboxEvent pendingEvent() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new OrderCreatedEvent(1L, new BigDecimal("100.00"), 2, "CREATED"));
        return OutboxEvent.of("Order", 1L, "order.created", payload);
    }

    @Test
    void publishPending_publicaEMarcaComoEnviado() throws Exception {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        outboxPublisher.publishPending();

        verify(orderEventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isNotNull();
    }

    @Test
    void publishPending_quandoPublicacaoFalha_mantemPendenteParaReprocessar() throws Exception {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("broker indisponivel"))
                .when(orderEventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));

        outboxPublisher.publishPending();

        // Nao marca como enviado -> sera reprocessado no proximo ciclo (nao perde o evento)
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getSentAt()).isNull();
    }
}

package com.aderson.ministore.outbox;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro da tabela de outbox. O evento e gravado aqui na MESMA transacao que
 * cria o pedido, garantindo atomicidade. Um publicador separado (OutboxPublisher)
 * le os pendentes e publica no RabbitMQ, marcando como enviados. Assim, mesmo com
 * o broker indisponivel, o evento nao se perde: fica persistido ate ser publicado.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 4000)
    private String payload;

    // Correlation id capturado no momento da criacao; sobrevive ao boundary
    // assincrono (poller/retry) e e propagado para o header da mensagem.
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant sentAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(String aggregateType, Long aggregateId, String type, String correlationId, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public static OutboxEvent of(String aggregateType, Long aggregateId, String type, String correlationId, String payload) {
        return new OutboxEvent(aggregateType, aggregateId, type, correlationId, payload);
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}

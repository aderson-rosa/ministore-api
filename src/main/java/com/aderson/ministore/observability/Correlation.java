package com.aderson.ministore.observability;

/**
 * Constantes do correlation id, usado para rastrear uma requisicao ponta a ponta
 * (HTTP -> criacao do pedido -> outbox -> mensagem RabbitMQ -> consumer).
 */
public final class Correlation {

    /** Chave no MDC (aparece nos logs). */
    public static final String MDC_KEY = "correlationId";

    /** Header HTTP e header da mensagem AMQP que carregam o id. */
    public static final String HEADER = "X-Correlation-Id";

    private Correlation() {
    }
}

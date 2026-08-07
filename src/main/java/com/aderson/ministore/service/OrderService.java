package com.aderson.ministore.service;

import com.aderson.ministore.config.RabbitConfig;
import com.aderson.ministore.domain.order.Order;
import com.aderson.ministore.domain.order.OrderItem;
import com.aderson.ministore.domain.order.OrderRepository;
import com.aderson.ministore.domain.product.Product;
import com.aderson.ministore.domain.product.ProductRepository;
import com.aderson.ministore.dto.CreateOrderRequest;
import com.aderson.ministore.dto.OrderItemRequest;
import com.aderson.ministore.dto.OrderResponse;
import com.aderson.ministore.exception.BusinessException;
import com.aderson.ministore.exception.NotFoundException;
import com.aderson.ministore.messaging.OrderCreatedEvent;
import com.aderson.ministore.outbox.OutboxEvent;
import com.aderson.ministore.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido nao encontrado: " + id));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order();

        for (OrderItemRequest itemRequest : request.items()) {
            // Lock pessimista: serializa o acesso ao produto para evitar race condition
            // na baixa de estoque quando dois pedidos chegam ao mesmo tempo.
            Product product = productRepository.findByIdForUpdate(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException("Produto nao encontrado: " + itemRequest.productId()));

            if (product.getStock() < itemRequest.quantity()) {
                throw new BusinessException(
                        "Estoque insuficiente para o produto '" + product.getName()
                                + "' (disponivel: " + product.getStock() + ", pedido: " + itemRequest.quantity() + ")");
            }

            product.decreaseStock(itemRequest.quantity());
            order.addItem(new OrderItem(product, itemRequest.quantity(), product.getPrice()));
        }

        Order saved = orderRepository.save(order);

        // Transactional Outbox: grava o evento na MESMA transacao do pedido.
        // O OutboxPublisher publica no RabbitMQ depois, garantindo que o evento
        // nao se perca mesmo que o broker esteja indisponivel neste momento.
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(), saved.getTotal(), saved.getItems().size(), saved.getStatus().name());
        outboxEventRepository.save(OutboxEvent.of(
                "Order", saved.getId(), RabbitConfig.ORDER_CREATED_ROUTING_KEY, toJson(event)));

        return OrderResponse.from(saved);
    }

    private String toJson(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o evento do pedido", e);
        }
    }
}

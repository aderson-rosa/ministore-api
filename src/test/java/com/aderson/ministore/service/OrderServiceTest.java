package com.aderson.ministore.service;

import com.aderson.ministore.config.RabbitConfig;
import com.aderson.ministore.domain.order.Order;
import com.aderson.ministore.domain.order.OrderRepository;
import com.aderson.ministore.domain.product.Product;
import com.aderson.ministore.domain.product.ProductRepository;
import com.aderson.ministore.dto.CreateOrderRequest;
import com.aderson.ministore.dto.OrderItemRequest;
import com.aderson.ministore.dto.OrderResponse;
import com.aderson.ministore.exception.BusinessException;
import com.aderson.ministore.exception.NotFoundException;
import com.aderson.ministore.outbox.OutboxEvent;
import com.aderson.ministore.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, outboxEventRepository,
                new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void create_comEstoqueSuficiente_baixaEstoqueEGravaNoOutbox() {
        Product product = new Product("Camiseta", "Algodao", new BigDecimal("50.00"), 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 2)));

        OrderResponse response = orderService.create(request);

        assertThat(product.getStock()).isEqualTo(8);
        assertThat(response.total()).isEqualByComparingTo("100.00");
        verify(orderRepository).save(any(Order.class));

        // Evento gravado no outbox (na mesma transacao), nao publicado diretamente
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RabbitConfig.ORDER_CREATED_ROUTING_KEY);
        assertThat(captor.getValue().getAggregateType()).isEqualTo("Order");
        assertThat(captor.getValue().getPayload()).contains("\"status\":\"CREATED\"");
    }

    @Test
    void create_comEstoqueInsuficiente_lancaBusinessException() {
        Product product = new Product("Tenis", "Corrida", new BigDecimal("300.00"), 1);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 5)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");

        assertThat(product.getStock()).isEqualTo(1); // nao alterou
        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void create_comProdutoInexistente_lancaNotFound() {
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(99L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}

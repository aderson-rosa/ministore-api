package com.aderson.ministore.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException("Produto nao encontrado: " + itemRequest.productId()));

            if (product.getStock() < itemRequest.quantity()) {
                throw new BusinessException(
                        "Estoque insuficiente para o produto '" + product.getName()
                                + "' (disponivel: " + product.getStock() + ", pedido: " + itemRequest.quantity() + ")");
            }

            product.decreaseStock(itemRequest.quantity());
            order.addItem(new OrderItem(product, itemRequest.quantity(), product.getPrice()));
        }

        return OrderResponse.from(orderRepository.save(order));
    }
}

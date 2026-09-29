package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order saveOrder(Order order, List<OrderLine> orderLines) {
        Order savedOrder = orderRepository.save(order);
        for (OrderLine orderLine : orderLines) {
            int quantity = orderLine.quantity();
            orderItemRepository.save(new OrderItem(
                    savedOrder.getId(), orderLine.product().getName(), orderLine.product().getPrice(), quantity));
        }
        return savedOrder;
    }
}
package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;

    public OrderPersistenceService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
    }

    public Order saveOrder(
            String customerFullName, String customerAddress, String customerPhone, List<OrderLine> orderLines) {
        Customer customer = customerRepository.findByFullNameAndAddressAndPhone(
                        customerFullName, customerAddress, customerPhone)
                .orElseGet(() -> customerRepository.save(
                        new Customer(customerFullName, customerAddress, customerPhone)));
        Order savedOrder = orderRepository.save(new Order(customer, "new"));
        for (OrderLine orderLine : orderLines) {
            int quantity = orderLine.quantity();
            orderItemRepository.save(new OrderItem(savedOrder.getId(), orderLine.product(), quantity));
        }
        return savedOrder;
    }
}
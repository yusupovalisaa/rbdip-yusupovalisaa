package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Used inside the orders controller to load associated product data.
    @EntityGraph(attributePaths = "product")
    List<OrderItem> findByOrderId(Long orderId);
}

package com.rbdip.bookstore.order;

import com.rbdip.bookstore.review.PurchaseLookup;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, PurchaseLookup {

    @Override
    @EntityGraph(attributePaths = "product")
    List<OrderItem> findAll();

    @Override
    @Query("""
            select count(item) > 0
            from OrderItem item
            join Order orderRecord on orderRecord.id = item.orderId
            join orderRecord.customer customer
            where item.product.id = :productId
                and case when customer.lastName = '' then customer.firstName
                    else concat(customer.firstName, ' ', customer.lastName) end = :customerName
            """)
    boolean hasPurchased(@Param("productId") Long productId, @Param("customerName") String customerName);
}

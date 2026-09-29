package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmationNotifier {

    public void sendOrderConfirmation(Order order, BigDecimal total) {
        System.out.printf(
                "[email] Dear %s, your order #%d for %s has been placed.%n",
                order.getCustomerFullName(), order.getId(), total);
    }
}
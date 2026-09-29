package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {

    public void validate(CreateOrderRequest request) {
        validateCustomerFullName(request.customerFullName());
        validateCustomerAddress(request.customerAddress());
        validateItems(request.items());
    }

    private void validateCustomerFullName(String customerFullName) {
        if (customerFullName == null || customerFullName.isBlank()) {
            throw new IllegalArgumentException("customerFullName is required");
        }
    }

    private void validateCustomerAddress(String customerAddress) {
        if (customerAddress == null || customerAddress.isBlank()) {
            throw new IllegalArgumentException("customerAddress is required");
        }
    }

    private void validateItems(List<CreateOrderRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }

        for (CreateOrderRequest.Item item : items) {
            if (item.quantity() != null && item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }
}
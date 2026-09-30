package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRequestValidator requestValidator;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistenceService persistenceService;
    private final OrderConfirmationNotifier confirmationNotifier;

    public OrderService(
            ProductRepository productRepository,
            OrderRequestValidator requestValidator,
            PricingCalculator pricingCalculator,
            OrderPersistenceService persistenceService,
            OrderConfirmationNotifier confirmationNotifier) {
        this.productRepository = productRepository;
        this.requestValidator = requestValidator;
        this.pricingCalculator = pricingCalculator;
        this.persistenceService = persistenceService;
        this.confirmationNotifier = confirmationNotifier;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        requestValidator.validate(request);

        List<OrderLine> orderLines = new ArrayList<>();
        List<PricingCalculator.LineItem> lineItems = new ArrayList<>();
        for (CreateOrderRequest.Item raw : request.items()) {
            Product product = productRepository.findById(raw.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + raw.productId() + " not found"));
            int quantity = raw.quantity() == null ? 1 : raw.quantity();
            orderLines.add(new OrderLine(product, quantity));
            lineItems.add(new PricingCalculator.LineItem(product.getPrice(), quantity));
        }

        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lineItems, request.customerType() == null ? "regular" : request.customerType(), request.couponCode());

        Order order = persistenceService.saveOrder(
            request.customerFullName(), request.customerAddress(), request.customerPhone(), orderLines);
        confirmationNotifier.sendOrderConfirmation(order, total);

        return order;
    }
}

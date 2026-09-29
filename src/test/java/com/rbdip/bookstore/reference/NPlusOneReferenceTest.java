package com.rbdip.bookstore.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Гейт ЛР4: GET /orders не должен выполнять отдельный SQL-запрос на
 * позиции для каждого заказа. Порог подобран так, что текущая
 * реализация (N+1) его гарантированно нарушает; после фикса запросов
 * должно быть O(1)/O(log n), а не O(n) от числа заказов.
 */
class NPlusOneReferenceTest extends AbstractIntegrationTest {

    private static final int MAX_ALLOWED_QUERIES = 3;
    private static final int ORDERS_COUNT = 5;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listingOrdersDoesNotIssueOneQueryPerOrder() {
        Long productId = createProduct();
        for (int i = 0; i < ORDERS_COUNT; i++) {
            createOrder(productId);
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        restTemplate.getForEntity("/orders", List.class);

        long queries = stats.getPrepareStatementCount();
        assertThat(queries)
                .as("GET /orders должен выполнять ограниченное число SQL-запросов независимо от числа заказов (сейчас %d заказов, %d запросов - похоже на N+1)", ORDERS_COUNT, queries)
                .isLessThanOrEqualTo(MAX_ALLOWED_QUERIES);
    }

    private Long createProduct() {
        Map<?, ?> body = restTemplate.postForEntity(
                        "/products", Map.of("name", "Domain-Driven Design", "price", new BigDecimal("50.00")), Map.class)
                .getBody();
        return Long.valueOf(body.get("id").toString());
    }

    private void createOrder(Long productId) {
        CreateOrderRequest request = new CreateOrderRequest(
                "Test Customer", "Test Address", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        restTemplate.postForEntity("/orders", request, Map.class);
    }
}

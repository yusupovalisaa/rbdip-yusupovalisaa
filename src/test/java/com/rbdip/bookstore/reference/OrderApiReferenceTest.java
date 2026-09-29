package com.rbdip.bookstore.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Эталонное поведение публичного API заказов. Не редактировать: любой
 * рефакторинг (ЛР1, ЛР4, ЛР5) обязан оставить эти тесты зелёными.
 */
class OrderApiReferenceTest extends AbstractIntegrationTest {

    @Test
    void createsOrderAndReturnsIt() {
        Long productId = createProduct("Clean Code", new BigDecimal("35.00"));

        CreateOrderRequest request = new CreateOrderRequest(
                "Ivan Petrov", "Moscow, Lenina 1", "+79990000000", "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 2)));

        ResponseEntity<Map> response = restTemplate.postForEntity("/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("status", "new");
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    void rejectsOrderWithoutItems() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Ivan Petrov", "Moscow, Lenina 1", null, "regular", null, List.of());

        ResponseEntity<Map> response = restTemplate.postForEntity("/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listsOrdersWithTheirItems() {
        Long productId = createProduct("Refactoring", new BigDecimal("40.00"));
        CreateOrderRequest request = new CreateOrderRequest(
                "Anna Sidorova", "SPb, Nevsky 10", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        restTemplate.postForEntity("/orders", request, Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity("/orders", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        Map<?, ?> order = (Map<?, ?>) response.getBody().get(0);
        assertThat(order.get("customerFullName")).isEqualTo("Anna Sidorova");
        assertThat((List<?>) order.get("items")).hasSize(1);
    }

    private Long createProduct(String name, BigDecimal price) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/products", Map.of("name", name, "price", price), Map.class);
        return Long.valueOf(response.getBody().get("id").toString());
    }
}

package com.rbdip.bookstore.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Гейт ЛР2: после нормализации схемы должна появиться отдельная таблица
 * customers, а orders/order_items - ссылаться на неё и на products по
 * внешнему ключу вместо дублирования данных. На старте (до того как
 * студент добавит свои Flyway-миграции V2/V3...) тест КРАСНЫЙ - это
 * ожидаемо и документирует задание, а не баг стартового проекта.
 */
class SchemaNormalizationReferenceTest extends AbstractIntegrationTest {

    @Test
    void ordersReferenceCustomersTableInsteadOfEmbeddingContactData() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables)
                .as("Ожидается отдельная таблица customers после нормализации схемы (ЛР2)")
                .contains("customers");

        List<String> orderColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'orders'",
                String.class);

        assertThat(orderColumns)
                .as("orders не должен хранить сырые контактные данные клиента после нормализации")
                .doesNotContain("customer_full_name", "customer_address", "customer_phone");

        List<String> orderItemColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'order_items'",
                String.class);

        assertThat(orderItemColumns)
                .as("order_items не должен дублировать название/цену товара после нормализации")
                .doesNotContain("product_name", "product_price");
    }
}

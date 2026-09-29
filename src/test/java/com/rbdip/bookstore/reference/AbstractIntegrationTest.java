package com.rbdip.bookstore.reference;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Базовый класс для эталонных (reference) интеграционных тестов. НЕ
 * редактируется студентами - проверяет, что внешнее поведение системы
 * не изменилось после рефакторинга.
 *
 * <p>Контейнер PostgreSQL - singleton на весь прогон тестов (а не по
 * одному на класс): так экономятся ресурсы CI-раннера и не бывает
 * гонок/обрывов соединений при параллельном старте нескольких БД.
 * Контейнер не останавливается явно - его убирает Ryuk (сервис-компаньон
 * Testcontainers) по завершении JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE reviews, order_items, orders, products RESTART IDENTITY CASCADE");
    }
}

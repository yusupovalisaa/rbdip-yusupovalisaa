package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.BookstoreApplication;
import com.rbdip.bookstore.order.CreateOrderRequest;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerNameMigrationLoadTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void servesOrdersDuringBackfillAndAfterContract() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).target("2").load().migrate();
        jdbc.update("INSERT INTO customers (full_name) VALUES ('Мария  Иванова Соколова'), ('Plato ')");
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        jdbc.update("INSERT INTO customers (full_name) VALUES ('Ada Byron')");
        jdbc.update("UPDATE customers SET full_name = 'Ada Lovelace' WHERE full_name = 'Ada Byron'");
        assertNameParts(jdbc, "Ada Lovelace", "Ada", "Lovelace");
        Long productId = jdbc.queryForObject("INSERT INTO products (name, price) VALUES ('Book', 10) RETURNING id",
                Long.class);

        try (var app = (ServletWebServerApplicationContext) new SpringApplicationBuilder(BookstoreApplication.class)
                .run("--server.port=0", "--spring.flyway.enabled=false",
                        "--spring.jpa.properties.hibernate.generate_statistics=false",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword());
                ExecutorService pool = Executors.newFixedThreadPool(4)) {
            TestRestTemplate api = new TestRestTemplate(new RestTemplateBuilder()
                    .rootUri("http://localhost:" + app.getWebServer().getPort())
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(5)));

            Future<?> backfill;
            try (Connection blocker = dataSource.getConnection(); Statement statement = blocker.createStatement()) {
                // Hold a historical row so the backfill is still running when the HTTP requests complete.
                blocker.setAutoCommit(false);
                statement.executeQuery("SELECT id FROM customers WHERE full_name = 'Plato ' FOR UPDATE").close();
                backfill = pool.submit(() -> Flyway.configure().dataSource(dataSource).target("4").load().migrate());
                awaitBackfillLock(jdbc);

                List<Future<?>> workers = List.of(
                        pool.submit(() -> exerciseOrders(api, productId, "Anna")),
                        pool.submit(() -> exerciseOrders(api, productId, "Ivan")),
                        pool.submit(() -> exerciseOrders(api, productId, "Maria")));
                for (Future<?> worker : workers) {
                    worker.get(20, TimeUnit.SECONDS);
                }
                assertNameParts(jdbc, "Anna Customer0", "Anna", "Customer0");
                assertThat(jdbc.queryForList("SELECT customer_full_name FROM orders", String.class))
                        .contains("Anna Customer0");
                assertThat(backfill.isDone()).isFalse();
                blocker.rollback();
            }
            backfill.get(15, TimeUnit.SECONDS);
            assertNameParts(jdbc, "Мария  Иванова Соколова", "Мария", " Иванова Соколова");
            assertNameParts(jdbc, "Plato ", "Plato ", "");
            jdbc.update("INSERT INTO orders (customer_id) SELECT id FROM customers WHERE full_name = 'Plato '");
            assertOrderListed(api, "Plato ");

            Flyway.configure().dataSource(dataSource).load().migrate();
            assertThat(jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'customers'
                    """, String.class)).contains("first_name", "last_name").doesNotContain("full_name");
            assertThat(jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'orders'
                    """, String.class)).doesNotContain("customer_full_name");
            exerciseOrders(api, productId, "Grace");
            assertOrderListed(api, "Anna Customer0");
            assertOrderListed(api, "Plato ");
        }
    }

    private void awaitBackfillLock(JdbcTemplate jdbc) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock' AND query LIKE '%UPDATE customers%'
                        AND pid <> pg_backend_pid())
                    """, Boolean.class))) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(25);
        }
        throw new AssertionError("The Flyway backfill did not reach the locked customer row");
    }

    private void exerciseOrders(TestRestTemplate api, Long productId, String firstName) {
        for (int i = 0; i < 5; i++) {
            String fullName = firstName + " Customer" + i;
            CreateOrderRequest request = new CreateOrderRequest(fullName, "Moscow", null, "regular", null,
                    List.of(new CreateOrderRequest.Item(productId, 1)));
            var response = api.postForEntity("/orders", request, String.class);
            assertThat(response.getStatusCode()).as("POST /orders: %s", response.getBody())
                    .isEqualTo(HttpStatus.CREATED);
            assertOrderListed(api, fullName);
        }
    }

    private void assertOrderListed(TestRestTemplate api, String fullName) {
        var response = api.getForEntity("/orders", String.class);
        assertThat(response.getStatusCode()).as("GET /orders: %s", response.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"customerFullName\":\"" + fullName + "\"");
    }

    private void assertNameParts(JdbcTemplate jdbc, String fullName, String firstName, String lastName) {
        assertThat(jdbc.queryForMap("SELECT first_name, last_name FROM customers WHERE full_name = ?", fullName))
                .containsAllEntriesOf(Map.of("first_name", firstName, "last_name", lastName));
    }
}

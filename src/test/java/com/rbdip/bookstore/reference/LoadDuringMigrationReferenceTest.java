package com.rbdip.bookstore.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Гейт ЛР3: миграции схемы должны выполняться без даунтайма (паттерн
 * expand-contract). Тест сознательно НЕ использует Spring-контекст
 * (Hibernate в режиме ddl-auto=validate не переживёт промежуточное
 * состояние схемы) - вместо этого он сам управляет накатом миграций
 * через Flyway API и параллельно шлёт "старые" запросы напрямую по JDBC.
 *
 * Конвенция для студентов (см. README в src/main/resources/db/migration):
 * последняя миграция в цепочке считается "contract"-шагом (удаление
 * старой колонки) и намеренно не покрывается этим тестом - его смысл
 * проверить, что все ШАГИ ДО контракта (expand + backfill/dual-write)
 * не ломают уже работающих потребителей старой колонки
 * customer_full_name.
 */
@Testcontainers
class LoadDuringMigrationReferenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_migration")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Test
    void expandStepsDoNotBreakReadsOfTheOldColumn() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        MigrationInfo[] pending = flyway.info().pending();
        if (pending.length <= 1) {
            // Студент ещё не добавил свои миграции (V2, V3, ...) - тест
            // нечего проверять на старте, гейт станет активным в ходе ЛР3.
            flyway.migrate();
            return;
        }

        // Базовая версия - до любых миграций студента (только V1__init.sql).
        Flyway.configure().dataSource(dataSource).target("1").load().migrate();

        String lastBeforeContract = pending[pending.length - 2].getVersion().getVersion();

        List<Exception> errors = new CopyOnWriteArrayList<>();
        AtomicBoolean keepReading = new AtomicBoolean(true);
        CountDownLatch started = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);

        Runnable reader = () -> {
            started.countDown();
            while (keepReading.get()) {
                try (Connection c = dataSource.getConnection();
                        Statement st = c.createStatement()) {
                    st.executeQuery("SELECT customer_full_name FROM orders LIMIT 1");
                } catch (SQLException e) {
                    errors.add(e);
                }
            }
        };
        for (int i = 0; i < 4; i++) {
            pool.submit(reader);
        }
        started.await(5, TimeUnit.SECONDS);

        // Все expand/backfill-миграции (всё, кроме последнего "contract"-шага)
        // накатываются, пока фоновые потоки продолжают читать старую колонку.
        Flyway.configure().dataSource(dataSource).target(lastBeforeContract).load().migrate();

        keepReading.set(false);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(errors)
                .as("Чтения по старой колонке customer_full_name не должны падать во время expand/backfill-миграций")
                .isEmpty();
    }
}

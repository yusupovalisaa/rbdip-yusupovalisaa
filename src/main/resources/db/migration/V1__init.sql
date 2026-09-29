-- Начальная ("грязная") схема учебного проекта Bookstore.
-- Намеренно денормализована: orders хранит "сырые" контактные данные
-- клиента напрямую, order_items дублирует название/цену товара вместо
-- ссылки на products. Это цель нормализации в ЛР2.

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    description TEXT
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_full_name VARCHAR(255) NOT NULL,
    customer_address VARCHAR(500),
    customer_phone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'new',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_name VARCHAR(255) NOT NULL,
    product_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    author_name VARCHAR(255),
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

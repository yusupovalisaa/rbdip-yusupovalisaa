CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;

UPDATE orders AS order_record
SET customer_id = customer.id
FROM customers AS customer
WHERE customer.full_name = order_record.customer_full_name
  AND customer.address IS NOT DISTINCT FROM order_record.customer_address
  AND customer.phone IS NOT DISTINCT FROM order_record.customer_phone;

ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE order_items ADD COLUMN product_id BIGINT;

-- V1 did not retain product IDs, so preserve unmatched historical snapshots as products.
INSERT INTO products (name, price)
SELECT DISTINCT item.product_name, item.product_price
FROM order_items AS item
WHERE NOT EXISTS (
    SELECT 1
    FROM products AS product
    WHERE product.name = item.product_name
      AND product.price = item.product_price
);

UPDATE order_items AS item
SET product_id = (
    SELECT MIN(product.id)
    FROM products AS product
    WHERE product.name = item.product_name
      AND product.price = item.product_price
);

ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE orders
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

ALTER TABLE order_items
    DROP COLUMN product_name,
    DROP COLUMN product_price;
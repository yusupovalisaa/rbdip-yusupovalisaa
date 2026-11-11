SET LOCAL lock_timeout = '2s';

LOCK TABLE orders, customers IN ACCESS EXCLUSIVE MODE NOWAIT;

DROP TRIGGER IF EXISTS fill_order_customer_full_name ON orders;
DROP FUNCTION IF EXISTS fill_order_customer_full_name();
ALTER TABLE orders DROP COLUMN IF EXISTS customer_full_name;

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

DROP TRIGGER sync_customer_names ON customers;
DROP FUNCTION sync_customer_names();

ALTER TABLE customers
    DROP CONSTRAINT customers_first_name_present,
    DROP CONSTRAINT customers_last_name_present,
    DROP COLUMN full_name;

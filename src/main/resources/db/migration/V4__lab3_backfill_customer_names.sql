UPDATE customers
SET full_name = full_name
WHERE first_name IS NULL OR last_name IS NULL;

ALTER TABLE customers
    VALIDATE CONSTRAINT customers_first_name_present,
    VALIDATE CONSTRAINT customers_last_name_present;

SET LOCAL lock_timeout = '2s';

ALTER TABLE customers
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255),
    ADD CONSTRAINT customers_first_name_present CHECK (first_name IS NOT NULL) NOT VALID,
    ADD CONSTRAINT customers_last_name_present CHECK (last_name IS NOT NULL) NOT VALID;

CREATE FUNCTION sync_customer_names() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    separator INTEGER;
BEGIN
    IF NEW.first_name IS NULL OR NEW.last_name IS NULL
        OR (TG_OP = 'UPDATE' AND NEW.full_name IS DISTINCT FROM OLD.full_name) THEN
        separator := strpos(NEW.full_name, ' ');
        IF separator > 0 AND separator < char_length(NEW.full_name) THEN
            NEW.first_name := left(NEW.full_name, separator - 1);
            NEW.last_name := substr(NEW.full_name, separator + 1);
        ELSE
            NEW.first_name := NEW.full_name;
            NEW.last_name := '';
        END IF;
    ELSE
        NEW.full_name := NEW.first_name || CASE WHEN NEW.last_name = '' THEN '' ELSE ' ' || NEW.last_name END;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER sync_customer_names
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION sync_customer_names();

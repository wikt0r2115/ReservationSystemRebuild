DROP INDEX IF EXISTS idx_reservation_customer_email;

UPDATE reservation SET customer_email = lower(customer_email);

CREATE INDEX IF NOT EXISTS idx_reservation_customer_email_created
    ON reservation (customer_email, created_at DESC);

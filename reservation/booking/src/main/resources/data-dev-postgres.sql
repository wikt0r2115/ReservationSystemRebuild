INSERT INTO availability_slot (id, offer_id, starts_at, ends_at, capacity, reserved_count, status)
VALUES (1, 1, '2099-06-02 10:00:00', '2099-06-02 12:00:00', 2, 0, 'OPEN')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('availability_slot', 'id'), (SELECT MAX(id) FROM availability_slot));

INSERT INTO offer (id, name, image_url, description, price, archived)
SELECT 900001, 'City Discovery Tour', '/demo-tour.svg',
       'A sample city tour for trying the reservation flow.', 149.00, false
WHERE NOT EXISTS (SELECT 1 FROM offer WHERE id = 900001);

INSERT INTO availability_slot (id, offer_id, starts_at, ends_at, capacity, reserved_count, status, version)
SELECT 900001, 900001, TIMESTAMP '2030-06-15 10:00:00', TIMESTAMP '2030-06-15 12:00:00',
       8, 0, 'OPEN', 0
WHERE NOT EXISTS (SELECT 1 FROM availability_slot WHERE id = 900001);

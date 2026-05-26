ALTER TABLE availability_slot
    ADD CONSTRAINT uq_availability_slot_offer_time
    UNIQUE (offer_id, starts_at, ends_at);

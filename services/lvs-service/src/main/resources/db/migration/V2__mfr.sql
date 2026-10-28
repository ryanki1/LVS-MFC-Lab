CREATE TABLE transport_order (
    id                 BIGSERIAL PRIMARY KEY,
    storage_order_id   BIGINT      NOT NULL UNIQUE,
    load_carrier_id    BIGINT      NOT NULL,
    source_location_id BIGINT,
    target_location_id BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'CREATED'
);

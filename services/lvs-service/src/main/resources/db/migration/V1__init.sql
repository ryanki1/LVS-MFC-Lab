-- Artikel
CREATE TABLE article (
    id             BIGSERIAL PRIMARY KEY,
    article_number VARCHAR(80)  NOT NULL UNIQUE,
    description    VARCHAR(200) NOT NULL,
    storage_type   VARCHAR(40)  NOT NULL DEFAULT 'STANDARD'
);

-- Lagerplätze
CREATE TABLE storage_location (
    id            BIGSERIAL PRIMARY KEY,
    aisle         INTEGER     NOT NULL,
    level         INTEGER     NOT NULL,
    slot          INTEGER     NOT NULL,
    location_type VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
    status        VARCHAR(20) NOT NULL DEFAULT 'FREE',
    UNIQUE (aisle, level, slot)
);

-- Ladungsträger
CREATE TABLE load_carrier (
    id           BIGSERIAL PRIMARY KEY,
    carrier_code VARCHAR(80) NOT NULL UNIQUE,
    article_id   BIGINT      NOT NULL REFERENCES article (id),
    quantity     INTEGER     NOT NULL,
    location_id  BIGINT               REFERENCES storage_location (id)
);

-- Avis (Advance Shipping Notice)
CREATE TABLE advance_shipping_notice (
    id            BIGSERIAL PRIMARY KEY,
    notice_number VARCHAR(80) NOT NULL UNIQUE
);

-- Avis-Positionen
CREATE TABLE asn_position (
    id                BIGSERIAL PRIMARY KEY,
    notice_id         BIGINT  NOT NULL REFERENCES advance_shipping_notice (id),
    article_id        BIGINT  NOT NULL REFERENCES article (id),
    expected_quantity INTEGER NOT NULL,
    received_quantity INTEGER NOT NULL DEFAULT 0
);

-- Einlageraufträge
CREATE TABLE storage_order (
    id               BIGSERIAL PRIMARY KEY,
    asn_position_id  BIGINT     NOT NULL REFERENCES asn_position (id),
    load_carrier_id  BIGINT     NOT NULL REFERENCES load_carrier (id),
    target_location_id BIGINT   NOT NULL REFERENCES storage_location (id),
    status           VARCHAR(20) NOT NULL DEFAULT 'CREATED'
);

-- Bestandsbewegungen
CREATE TABLE stock_movement (
    id                 BIGSERIAL PRIMARY KEY,
    load_carrier_id    BIGINT      NOT NULL REFERENCES load_carrier (id),
    article_id         BIGINT      NOT NULL REFERENCES article (id),
    quantity           INTEGER     NOT NULL,
    source_location_id BIGINT               REFERENCES storage_location (id),
    target_location_id BIGINT               REFERENCES storage_location (id),
    moved_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Testdaten: zwei Gassen mit je 5 Plätzen (Ebene 1, Slots 1–5)
INSERT INTO storage_location (aisle, level, slot, location_type, status)
SELECT aisle, 1, slot, 'STANDARD', 'FREE'
FROM generate_series(1, 2) AS aisle,
     generate_series(1, 5) AS slot;

-- Ein Kaltlager-Platz in Gasse 1
INSERT INTO storage_location (aisle, level, slot, location_type, status)
VALUES (1, 2, 1, 'COLD', 'FREE');

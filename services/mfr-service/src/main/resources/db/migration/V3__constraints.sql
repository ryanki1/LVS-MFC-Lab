-- stock_movement.target_location_id: fachlich immer vorhanden bei einer Einlagerung
ALTER TABLE stock_movement
    ALTER COLUMN target_location_id SET NOT NULL;

-- asn_position: Positionen gehören zum Avis – beim Löschen des Avis mitlöschen
ALTER TABLE asn_position
    DROP CONSTRAINT asn_position_notice_id_fkey,
    ADD CONSTRAINT asn_position_notice_id_fkey
        FOREIGN KEY (notice_id) REFERENCES advance_shipping_notice (id) ON DELETE CASCADE;

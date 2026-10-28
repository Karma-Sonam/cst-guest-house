ALTER TABLE rooms ADD COLUMN location VARCHAR(120);
ALTER TABLE rooms ADD COLUMN description VARCHAR(1000);
ALTER TABLE rooms ADD COLUMN amenities VARCHAR(500);
CREATE UNIQUE INDEX rooms_number_normalized_idx ON rooms(lower(room_number));

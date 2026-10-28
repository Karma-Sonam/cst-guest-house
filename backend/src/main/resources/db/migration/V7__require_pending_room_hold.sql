-- New and updated pending requests require a concrete room. Preserve unallocatable
-- legacy rows for explicit administrative approval/allocation or rejection.
ALTER TABLE bookings ADD CONSTRAINT pending_room_requires_hold
CHECK (status<>'PENDING' OR assigned_room_id IS NOT NULL) NOT VALID;

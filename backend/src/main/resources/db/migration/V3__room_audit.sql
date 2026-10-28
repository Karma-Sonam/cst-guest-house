ALTER TABLE bookings ALTER COLUMN admin_remark TYPE VARCHAR(1500);
CREATE TABLE audit_log (
 id UUID PRIMARY KEY,
 actor_id UUID NOT NULL REFERENCES app_users(id),
 action VARCHAR(60) NOT NULL,
 entity_id UUID NOT NULL,
 description VARCHAR(1000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL
);

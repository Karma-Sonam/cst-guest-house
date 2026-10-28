CREATE TABLE app_users (
 id UUID PRIMARY KEY,
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE,
 phone VARCHAR(30),
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(10) NOT NULL CHECK (role IN ('USER','ADMIN')),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK (email = lower(email))
);
CREATE TABLE auth_sessions (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_users(id),
 expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX auth_sessions_user_idx ON auth_sessions(user_id);
CREATE TABLE rooms (
 id UUID PRIMARY KEY,
 room_number VARCHAR(20) NOT NULL UNIQUE,
 category VARCHAR(10) NOT NULL CHECK (category IN ('STANDARD','SPECIAL')),
 standard_beds INTEGER,
 normal_capacity INTEGER,
 max_capacity INTEGER,
 supports_extra_bed BOOLEAN NOT NULL,
 service_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
   CHECK (service_status IN ('ACTIVE','MAINTENANCE','UNAVAILABLE')),
 CHECK (normal_capacity IS NULL OR normal_capacity > 0),
 CHECK (max_capacity IS NULL OR max_capacity >= normal_capacity)
);
INSERT INTO rooms (id,room_number,category,standard_beds,normal_capacity,max_capacity,supports_extra_bed)
SELECT ('00000000-0000-0000-0000-' || lpad(n::text,12,'0'))::uuid,
 n::text,'STANDARD',2,2,3,TRUE FROM generate_series(101,108) n;
INSERT INTO rooms (id,room_number,category,supports_extra_bed)
VALUES ('00000000-0000-0000-0000-000000000201','201','SPECIAL',FALSE),
 ('00000000-0000-0000-0000-000000000202','202','SPECIAL',FALSE);
CREATE TABLE facilities (
 id UUID PRIMARY KEY,
 type VARCHAR(20) NOT NULL UNIQUE CHECK (type IN ('CONVENTION','DINING')),
 name VARCHAR(100) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO facilities(id,type,name) VALUES
 ('00000000-0000-0000-0001-000000000001','CONVENTION','Convention Hall'),
 ('00000000-0000-0000-0001-000000000002','DINING','Dining Hall');

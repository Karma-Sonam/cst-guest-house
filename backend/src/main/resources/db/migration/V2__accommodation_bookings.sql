CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE bookings (
 id UUID PRIMARY KEY,
 reference VARCHAR(40) NOT NULL UNIQUE,
 user_id UUID NOT NULL REFERENCES app_users(id),
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(254) NOT NULL,
 phone VARCHAR(30),
 organization VARCHAR(160),
 designation VARCHAR(120),
 guest_category VARCHAR(40) NOT NULL,
 purpose VARCHAR(1000) NOT NULL,
 guests INTEGER NOT NULL CHECK (guests BETWEEN 1 AND 3),
 check_in DATE NOT NULL,
 check_out DATE NOT NULL CHECK (check_out > check_in),
 preferred_category VARCHAR(10) NOT NULL CHECK (preferred_category IN ('STANDARD','SPECIAL')),
 assigned_room_id UUID REFERENCES rooms(id),
 extra_bed_requested BOOLEAN NOT NULL,
 extra_bed_decision VARCHAR(20) NOT NULL CHECK (extra_bed_decision IN ('NOT_REQUESTED','PENDING','APPROVED','REJECTED')),
 extra_bed_added BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','CHECKED_IN','COMPLETED')),
 user_remark VARCHAR(1000),
 admin_remark VARCHAR(1000),
 actual_check_in TIMESTAMPTZ,
 actual_check_out TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 CHECK (status NOT IN ('APPROVED','CHECKED_IN') OR assigned_room_id IS NOT NULL),
 EXCLUDE USING gist (assigned_room_id WITH =, daterange(check_in,check_out,'[)') WITH &&)
   WHERE (status IN ('APPROVED','CHECKED_IN'))
);
CREATE INDEX bookings_user_idx ON bookings(user_id,created_at);
CREATE TABLE booking_status_history (
 id UUID PRIMARY KEY,
 booking_id UUID NOT NULL REFERENCES bookings(id),
 previous_status VARCHAR(20),
 new_status VARCHAR(20) NOT NULL,
 changed_by UUID NOT NULL REFERENCES app_users(id),
 remark VARCHAR(1500),
 changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX booking_history_idx ON booking_status_history(booking_id,changed_at);

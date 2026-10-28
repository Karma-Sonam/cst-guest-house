CREATE TABLE facility_requests (
 id UUID PRIMARY KEY,
 facility_id UUID NOT NULL REFERENCES facilities(id),
 user_id UUID NOT NULL REFERENCES app_users(id),
 booking_id UUID REFERENCES bookings(id),
 title VARCHAR(160) NOT NULL,
 purpose VARCHAR(1000) NOT NULL,
 usage_date DATE NOT NULL,
 start_time TIME NOT NULL,
 end_time TIME NOT NULL CHECK (end_time > start_time),
 meal_period VARCHAR(20) CHECK (meal_period IN ('BREAKFAST','LUNCH','DINNER','OTHER')),
 participants INTEGER NOT NULL CHECK (participants > 0),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED')),
 user_remark VARCHAR(1000),
 admin_remark VARCHAR(1000),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 EXCLUDE USING gist (facility_id WITH =, tsrange(usage_date+start_time,usage_date+end_time,'[)') WITH &&)
   WHERE (status = 'APPROVED')
);
CREATE INDEX facility_requests_user_idx ON facility_requests(user_id,created_at);
CREATE INDEX facility_requests_booking_idx ON facility_requests(booking_id);
CREATE TABLE facility_request_history (
 id UUID PRIMARY KEY,
 request_id UUID NOT NULL REFERENCES facility_requests(id),
 previous_status VARCHAR(20),
 new_status VARCHAR(20) NOT NULL,
 changed_by UUID NOT NULL REFERENCES app_users(id),
 remark VARCHAR(1000),
 changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX facility_history_request_idx ON facility_request_history(request_id,changed_at);

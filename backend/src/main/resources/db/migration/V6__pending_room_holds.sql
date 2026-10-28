DO $$
DECLARE constraint_name text;
BEGIN
  FOR constraint_name IN SELECT conname FROM pg_constraint WHERE conrelid='bookings'::regclass AND contype='x'
  LOOP EXECUTE format('ALTER TABLE bookings DROP CONSTRAINT %I',constraint_name); END LOOP;
END $$;

ALTER TABLE bookings ADD CONSTRAINT bookings_room_hold_excl
EXCLUDE USING gist (assigned_room_id WITH =, daterange(check_in,check_out,'[)') WITH &&)
WHERE (status IN ('PENDING','APPROVED','CHECKED_IN'));

-- Preserve existing requests and assign eligible legacy pending requests a hold where possible.
-- Unallocatable legacy requests remain explicitly awaiting allocation for administrative review.
DO $$
DECLARE pending record; candidate uuid;
BEGIN
  FOR pending IN SELECT * FROM bookings WHERE status='PENDING' AND assigned_room_id IS NULL ORDER BY created_at,id
  LOOP
    SELECT r.id INTO candidate FROM rooms r
    WHERE r.category=pending.preferred_category AND r.service_status='ACTIVE'
      AND r.normal_capacity IS NOT NULL AND r.max_capacity>=pending.guests
      AND (r.normal_capacity>=pending.guests OR (r.supports_extra_bed AND pending.extra_bed_requested))
      AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.assigned_room_id=r.id
        AND b.status IN ('PENDING','APPROVED','CHECKED_IN')
        AND b.check_in<pending.check_out AND b.check_out>pending.check_in)
    ORDER BY r.id LIMIT 1;
    IF candidate IS NOT NULL THEN
      UPDATE bookings SET assigned_room_id=candidate WHERE id=pending.id;
      INSERT INTO booking_status_history(id,booking_id,previous_status,new_status,changed_by,remark,changed_at)
      VALUES (gen_random_uuid(),pending.id,'PENDING','PENDING',pending.user_id,'System migration: available room held pending review',CURRENT_TIMESTAMP);
    END IF;
  END LOOP;
END $$;

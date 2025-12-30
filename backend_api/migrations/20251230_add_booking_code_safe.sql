-- Migration Script: Add booking_code column to bookings table
-- Run this when database is ready

-- Step 1: Check if column already exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'bookings' AND column_name = 'booking_code'
  ) THEN
    -- Step 2: Add the column
    ALTER TABLE bookings ADD COLUMN booking_code VARCHAR(20) UNIQUE;

    -- Step 3: Create index for fast lookup
    CREATE INDEX idx_bookings_booking_code ON bookings(booking_code);

    -- Step 4: Populate existing bookings with codes
    UPDATE bookings
    SET booking_code = 'XK' || LPAD(CAST(trip_id AS VARCHAR), 4, '0') || LPAD(CAST(id AS VARCHAR), 4, '0')
    WHERE booking_code IS NULL
      AND id IS NOT NULL;

    -- Step 5: Make column NOT NULL after populating
    ALTER TABLE bookings ALTER COLUMN booking_code SET NOT NULL;

    RAISE NOTICE 'Migration completed: booking_code column added and populated';
  ELSE
    RAISE NOTICE 'Column booking_code already exists, skipping migration';
  END IF;
END $$;


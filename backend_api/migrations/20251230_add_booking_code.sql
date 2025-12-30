-- Add booking_code column for ticket verification
-- This code can be scanned/read if customer forgets their phone

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS booking_code VARCHAR(20) UNIQUE;

-- Create index for faster lookup (safe - won't fail if already exists)
CREATE INDEX IF NOT EXISTS idx_bookings_booking_code ON bookings(booking_code);

-- Add comment
COMMENT ON COLUMN bookings.booking_code IS 'Unique booking code for ticket verification (QR code). Format: TRIP_ID + timestamp hash.';

-- Populate booking_code for existing bookings that don't have one
-- Format: XK + padded trip_id (4 digits) + padded booking_id (4 digits)
UPDATE bookings
SET booking_code = 'XK' || LPAD(CAST(trip_id AS VARCHAR), 4, '0') || LPAD(CAST(id AS VARCHAR), 4, '0')
WHERE booking_code IS NULL
  AND id IS NOT NULL;

-- Optional: Make the column NOT NULL after populating existing records
-- ALTER TABLE bookings ALTER COLUMN booking_code SET NOT NULL;



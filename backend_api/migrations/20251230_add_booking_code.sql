-- Add booking_code column for ticket verification
-- This code can be scanned/read if customer forgets their phone

ALTER TABLE bookings
ADD COLUMN booking_code VARCHAR(20) UNIQUE;

-- Create index for faster lookup
CREATE INDEX idx_bookings_booking_code ON bookings(booking_code);

-- Add comment
COMMENT ON COLUMN bookings.booking_code IS 'Unique booking code for ticket verification (QR code). Format: TRIP_ID + timestamp hash.';


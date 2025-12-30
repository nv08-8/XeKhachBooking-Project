#!/bin/bash

# Database migration script for booking_code feature
# Run this on the production database after deploying the code changes

echo "🔄 Running booking_code migration..."

psql << EOF
-- Add booking_code column if it doesn't exist
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS booking_code VARCHAR(20) UNIQUE;

-- Create index for faster lookup if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_bookings_booking_code ON bookings(booking_code);

-- Add comment
COMMENT ON COLUMN bookings.booking_code IS 'Unique booking code for ticket verification (QR code). Format: XK + trip_id + timestamp hash.';

-- Populate booking_code for existing bookings that don't have one
UPDATE bookings
SET booking_code = 'XK' || LPAD(CAST(trip_id AS VARCHAR), 4, '0') || LPAD(CAST(id AS VARCHAR), 4, '0')
WHERE booking_code IS NULL;

-- Make the column NOT NULL after populating
ALTER TABLE bookings
ALTER COLUMN booking_code SET NOT NULL;

echo "✅ Migration completed successfully!"
EOF


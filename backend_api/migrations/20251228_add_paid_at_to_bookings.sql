-- Add paid_at column to bookings table
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE;

-- Create index for paid_at for better query performance
CREATE INDEX IF NOT EXISTS idx_bookings_paid_at ON bookings(paid_at);


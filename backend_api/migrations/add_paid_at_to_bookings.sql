-- Migration: add paid_at column to bookings
-- Run: psql -d <db> -f add_paid_at_to_bookings.sql

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS paid_at timestamptz NULL;

-- Optionally add an index if you query by paid_at frequently
-- CREATE INDEX IF NOT EXISTS idx_bookings_paid_at ON bookings(paid_at);


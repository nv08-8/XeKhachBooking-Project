-- Migration: Allow NULL trip_id in bookings table
-- Purpose: Allow trip_id to be NULL so we can delete trips while keeping booking history
-- Date: 2026-01-03

-- Drop the NOT NULL constraint on trip_id column
ALTER TABLE bookings ALTER COLUMN trip_id DROP NOT NULL;

-- Verify the change
-- SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name='bookings' AND column_name='trip_id';


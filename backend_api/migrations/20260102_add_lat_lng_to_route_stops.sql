-- Migration: Add latitude and longitude columns to route_stops table
-- Date: 2026-01-02
-- Description: Add geographic coordinates to route stops for map display functionality

-- Check if columns already exist before adding them
ALTER TABLE route_stops
ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8) DEFAULT 0,
ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8) DEFAULT 0;

-- Add comment to explain the columns
COMMENT ON COLUMN route_stops.latitude IS 'Latitude coordinate for map display (degrees)';
COMMENT ON COLUMN route_stops.longitude IS 'Longitude coordinate for map display (degrees)';

-- Create index on coordinates for faster map queries if needed later
CREATE INDEX IF NOT EXISTS idx_route_stops_coordinates
ON route_stops(latitude, longitude)
WHERE latitude != 0 AND longitude != 0;

-- Commit message
-- ✅ Successfully added latitude and longitude columns to route_stops table


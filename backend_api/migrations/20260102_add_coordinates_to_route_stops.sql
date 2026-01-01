-- Migration: Add latitude and longitude to route_stops table
-- Date: 2026-01-02

-- Add latitude and longitude columns to route_stops
ALTER TABLE route_stops
ADD COLUMN latitude DECIMAL(10, 8),
ADD COLUMN longitude DECIMAL(11, 8);

-- Add comment for documentation
COMMENT ON COLUMN route_stops.latitude IS 'Latitude coordinate for pickup/dropoff location';
COMMENT ON COLUMN route_stops.longitude IS 'Longitude coordinate for pickup/dropoff location';

-- Create indexes for better query performance (optional)
CREATE INDEX idx_route_stops_coordinates ON route_stops(latitude, longitude);


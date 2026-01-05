-- Migration: Fix paid_at timezone offset (+7 hours for Asia/Ho_Chi_Minh)
-- Purpose: Update all existing paid_at values by adding 7 hours to correct timezone offset
-- Date: 2026-01-06
-- Note: This is a one-time fix for historical data before the code fix was deployed

BEGIN;

-- Log before count
SELECT
    COUNT(*) AS records_with_paid_at_before,
    MIN(paid_at) AS earliest_paid_at,
    MAX(paid_at) AS latest_paid_at
FROM bookings
WHERE paid_at IS NOT NULL;

-- Update paid_at by adding 7 hours to all non-null values
UPDATE bookings
SET paid_at = paid_at + INTERVAL '7 hours'
WHERE paid_at IS NOT NULL;

-- Log after count to verify
SELECT
    COUNT(*) AS records_with_paid_at_after,
    MIN(paid_at) AS earliest_paid_at_new,
    MAX(paid_at) AS latest_paid_at_new
FROM bookings
WHERE paid_at IS NOT NULL;

-- Add a comment to the column to document the fix
COMMENT ON COLUMN bookings.paid_at IS 'Payment timestamp in Asia/Ho_Chi_Minh timezone (UTC+7). Historical data fixed on 2026-01-06 by adding 7 hours offset.';

COMMIT;


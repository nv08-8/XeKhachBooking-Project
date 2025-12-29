-- Allow NULL user_id for admin-created bookings (walk-in customers)
ALTER TABLE bookings
ALTER COLUMN user_id DROP NOT NULL;

-- Add a comment to explain this change
COMMENT ON COLUMN bookings.user_id IS 'User ID of the customer who booked. NULL for admin-created bookings for walk-in customers.';


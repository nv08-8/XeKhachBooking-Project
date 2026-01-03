-- Direct SQL to manually setup soft delete if migration fails
-- Can run this directly on the database

-- Add status column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'active';

-- Update existing users to have 'active' status if NULL
UPDATE users SET status = 'active' WHERE status IS NULL;

-- Make status NOT NULL
ALTER TABLE users ALTER COLUMN status SET NOT NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Verify the changes
SELECT COUNT(*), status FROM users GROUP BY status;


-- Add soft delete support to users table
-- Allows admins to delete user accounts while preserving booking history

-- Check if status column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'users' AND column_name = 'status') THEN
        ALTER TABLE users ADD COLUMN status VARCHAR(50) DEFAULT 'active';
    END IF;
END $$;

-- Add comment to status column explaining soft delete usage
COMMENT ON COLUMN users.status IS 'User account status: active, inactive, or deleted. Deleted users cannot login but their booking history is preserved.';

-- Optional: Create an index on status for faster queries filtering deleted users
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);


ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'active';
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);


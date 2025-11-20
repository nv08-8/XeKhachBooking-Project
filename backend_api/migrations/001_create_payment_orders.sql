-- backend_api/migrations/001_create_payment_orders.sql
CREATE TABLE IF NOT EXISTS payment_orders (
  id SERIAL PRIMARY KEY,
  order_code TEXT UNIQUE,
  booking_ids JSONB,
  amount NUMERIC,
  created_at TIMESTAMPTZ DEFAULT NOW()
);


-- Migration: Create payment_orders table for PayOS integration
-- Created: 2025-11-21
-- Description: Store mapping between PayOS order codes and booking IDs

CREATE TABLE IF NOT EXISTS payment_orders (
    id SERIAL PRIMARY KEY,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    booking_ids JSONB NOT NULL,
    amount INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_payment_orders_order_code ON payment_orders(order_code);
CREATE INDEX IF NOT EXISTS idx_payment_orders_created_at ON payment_orders(created_at DESC);

-- Add trigger to update updated_at automatically
CREATE OR REPLACE FUNCTION update_payment_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_payment_orders_updated_at
BEFORE UPDATE ON payment_orders
FOR EACH ROW
EXECUTE FUNCTION update_payment_orders_updated_at();

-- Sample query to check payment orders
-- SELECT po.*,
--        (SELECT jsonb_agg(b.*)
--         FROM bookings b
--         WHERE b.id IN (SELECT jsonb_array_elements_text(po.booking_ids)::int)) as bookings
-- FROM payment_orders po
-- ORDER BY po.created_at DESC
-- LIMIT 10;

COMMENT ON TABLE payment_orders IS 'Maps PayOS order codes to booking IDs for payment tracking';
COMMENT ON COLUMN payment_orders.order_code IS 'Unique order code sent to PayOS (timestamp)';
COMMENT ON COLUMN payment_orders.booking_ids IS 'JSON array of booking IDs associated with this payment';
COMMENT ON COLUMN payment_orders.amount IS 'Total payment amount in VND';
COMMENT ON COLUMN payment_orders.status IS 'Payment status: pending, completed, failed, cancelled';


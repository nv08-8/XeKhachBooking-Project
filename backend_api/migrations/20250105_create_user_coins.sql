-- Table to store user coin balance
CREATE TABLE IF NOT EXISTS user_coins (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    balance INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT NOW()
);

-- Table to store coin history (earning/spending)
CREATE TABLE IF NOT EXISTS coin_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id INTEGER REFERENCES bookings(id) ON DELETE SET NULL,
    amount INTEGER NOT NULL, -- positive for earn, negative for spend
    type VARCHAR(20) NOT NULL, -- 'earn', 'spend', 'refund'
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 1. Hàm xử lý cộng xu tự động (Hỗ trợ cả Insert và Update)
CREATE OR REPLACE FUNCTION reward_booking_coins()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.status IN ('confirmed', 'completed')) THEN
        IF (TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND (OLD.status IS NULL OR OLD.status != NEW.status))) THEN
            IF NOT EXISTS (SELECT 1 FROM coin_history WHERE booking_id = NEW.id AND type = 'earn') THEN
                -- Cộng 10,000 xu vào balance
                INSERT INTO user_coins (user_id, balance, last_updated)
                VALUES (NEW.user_id, 10000, NOW())
                ON CONFLICT (user_id) DO UPDATE 
                SET balance = user_coins.balance + 10000, last_updated = NOW();

                -- Lưu lịch sử
                INSERT INTO coin_history (user_id, booking_id, amount, type, description)
                VALUES (NEW.user_id, NEW.id, 10000, 'earn', 'Thưởng đặt vé thành công');
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Gán trigger vào bảng bookings cho cả INSERT và UPDATE
DROP TRIGGER IF EXISTS trg_reward_coins ON bookings;
CREATE TRIGGER trg_reward_coins
AFTER INSERT OR UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION reward_booking_coins();

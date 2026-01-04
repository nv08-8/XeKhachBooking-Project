-- Function to reward coins
CREATE OR REPLACE FUNCTION reward_booking_coins()
RETURNS TRIGGER AS $$
BEGIN
    -- Reward 10,000 coins when booking is confirmed or completed from pending
    IF (NEW.status IN ('confirmed', 'completed')) AND (OLD.status = 'pending' OR OLD.status IS NULL) THEN
        -- Check if already rewarded for this booking to avoid double reward
        IF NOT EXISTS (SELECT 1 FROM coin_history WHERE booking_id = NEW.id AND type = 'earn') THEN
            -- Update balance
            INSERT INTO user_coins (user_id, balance, last_updated)
            VALUES (NEW.user_id, 10000, NOW())
            ON CONFLICT (user_id) DO UPDATE 
            SET balance = user_coins.balance + 10000, last_updated = NOW();

            -- Insert history
            INSERT INTO coin_history (user_id, booking_id, amount, type, description)
            VALUES (NEW.user_id, NEW.id, 10000, 'earn', 'Thưởng đặt vé thành công');
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger on bookings table
DROP TRIGGER IF EXISTS trg_reward_coins ON bookings;
CREATE TRIGGER trg_reward_coins
AFTER UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION reward_booking_coins();

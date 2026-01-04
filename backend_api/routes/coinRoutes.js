const express = require("express");
const router = express.Router();
const db = require("../db");

// Lấy số dư xu
router.get("/coins/balance", async (req, res) => {
    const { user_id } = req.query;
    try {
        const { rows } = await db.query("SELECT balance FROM user_coins WHERE user_id = $1", [user_id]);
        res.json({ balance: rows.length > 0 ? rows[0].balance : 0 });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Xem lịch sử xu
router.get("/coins/history", async (req, res) => {
    const { user_id } = req.query;
    try {
        const { rows } = await db.query(
            "SELECT * FROM coin_history WHERE user_id = $1 ORDER BY created_at DESC",
            [user_id]
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API trừ xu khi sử dụng (Gọi lúc bấm thanh toán)
router.post("/coins/use", async (req, res) => {
    const { user_id, amount, booking_id } = req.body;
    console.log(`📥 /coins/use called with: user_id=${user_id}, amount=${amount}, booking_id=${booking_id}`);

    try {
        const client = await db.connect();
        await client.query("BEGIN");
        
        // Lock user's coins to prevent race conditions
        const coinRes = await client.query("SELECT balance FROM user_coins WHERE user_id = $1 FOR UPDATE", [user_id]);
        const balance = coinRes.rows.length > 0 ? coinRes.rows[0].balance : 0;
        console.log(`💰 User ${user_id} current balance: ${balance}, requesting to deduct: ${amount}`);

        if (balance < amount) {
            await client.query("ROLLBACK");
            client.release();
            console.log(`❌ Insufficient coins: balance=${balance} < amount=${amount}`);
            return res.status(400).json({ message: "Không đủ xu" });
        }

        // Deduct coins from user account
        const updateCoinRes = await client.query("UPDATE user_coins SET balance = balance - $1 WHERE user_id = $2 RETURNING balance", [amount, user_id]);
        console.log(`✅ Updated user_coins: new balance = ${updateCoinRes.rows[0]?.balance}`);

        // Record coin usage in coin_history
        const historyRes = await client.query(
            "INSERT INTO coin_history (user_id, booking_id, amount, type, description) VALUES ($1, $2, $3, 'spend', 'Sử dụng xu giảm giá') RETURNING id",
            [user_id, booking_id, -amount]
        );
        console.log(`✅ Inserted coin_history record: id=${historyRes.rows[0]?.id}`);

        // Update total_amount in bookings to reflect coin discount
        if (booking_id && amount > 0) {
            console.log(`🔄 Attempting to update booking ${booking_id}: reduce total_amount by ${amount}`);

            // First check if booking exists
            const bookingCheckRes = await client.query("SELECT id, total_amount FROM bookings WHERE id = $1", [booking_id]);
            if (bookingCheckRes.rowCount === 0) {
                console.log(`⚠️  Booking ${booking_id} not found!`);
            } else {
                const oldTotal = bookingCheckRes.rows[0].total_amount;
                console.log(`   Current total_amount: ${oldTotal}`);

                const updateBookingRes = await client.query(
                    "UPDATE bookings SET total_amount = total_amount - $1 WHERE id = $2 RETURNING total_amount",
                    [amount, booking_id]
                );

                if (updateBookingRes.rowCount > 0) {
                    const newTotal = updateBookingRes.rows[0].total_amount;
                    console.log(`✅ Updated booking ${booking_id}: ${oldTotal} → ${newTotal}`);
                } else {
                    console.log(`❌ Failed to update booking ${booking_id}`);
                }
            }
        } else {
            console.log(`⚠️  Skipping booking update: booking_id=${booking_id}, amount=${amount}`);
        }

        await client.query("COMMIT");
        client.release();
        console.log(`✅ Transaction committed successfully`);
        res.json({ message: "Sử dụng xu thành công", coin_deducted: amount });
    } catch (err) {
        console.error("❌ Error in /coins/use:", err);
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;

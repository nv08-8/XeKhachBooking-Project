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
    try {
        const client = await db.connect();
        await client.query("BEGIN");
        
        // Lock user's coins to prevent race conditions
        const coinRes = await client.query("SELECT balance FROM user_coins WHERE user_id = $1 FOR UPDATE", [user_id]);
        const balance = coinRes.rows.length > 0 ? coinRes.rows[0].balance : 0;

        if (balance < amount) {
            await client.query("ROLLBACK");
            client.release();
            return res.status(400).json({ message: "Không đủ xu" });
        }

        // Deduct coins from user account
        await client.query("UPDATE user_coins SET balance = balance - $1 WHERE user_id = $2", [amount, user_id]);

        // Record coin usage in coin_history
        await client.query(
            "INSERT INTO coin_history (user_id, booking_id, amount, type, description) VALUES ($1, $2, $3, 'spend', 'Sử dụng xu giảm giá')",
            [user_id, booking_id, -amount]
        );

        // Update total_amount in bookings to reflect coin discount
        if (booking_id && amount > 0) {
            await client.query(
                "UPDATE bookings SET total_amount = total_amount - $1 WHERE id = $2",
                [amount, booking_id]
            );
            console.log(`✅ Updated booking ${booking_id}: reduced total_amount by ${amount} xu`);
        }

        await client.query("COMMIT");
        client.release();
        res.json({ message: "Sử dụng xu thành công", coin_deducted: amount });
    } catch (err) {
        console.error("Error in /coins/use:", err);
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;

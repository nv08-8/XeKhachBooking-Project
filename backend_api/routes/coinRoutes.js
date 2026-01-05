const express = require("express");
const router = express.Router();
const db = require("../db");

// Helper to format date to Vietnam timezone (UTC+7)
function formatVietnamTime(date) {
  try {
    if (!date) return null;
    const d = new Date(date instanceof Date ? date : new Date(date));
    if (isNaN(d.getTime())) return null;

    // Convert to Vietnam time (UTC+7) by adding 7 hours
    const vietnamTime = new Date(d.getTime() + 7 * 60 * 60 * 1000);

    // Format as ISO string: YYYY-MM-DDTHH:mm:ss.sssZ
    const year = vietnamTime.getUTCFullYear();
    const month = String(vietnamTime.getUTCMonth() + 1).padStart(2, '0');
    const day = String(vietnamTime.getUTCDate()).padStart(2, '0');
    const hours = String(vietnamTime.getUTCHours()).padStart(2, '0');
    const minutes = String(vietnamTime.getUTCMinutes()).padStart(2, '0');
    const seconds = String(vietnamTime.getUTCSeconds()).padStart(2, '0');
    const ms = String(vietnamTime.getUTCMilliseconds()).padStart(3, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}.${ms}Z`;
  } catch (e) {
    console.warn('Failed to format Vietnam time:', e);
    return null;
  }
}

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
        // Format created_at to Vietnam timezone
        const formattedRows = rows.map(row => ({
            ...row,
            created_at: row.created_at ? formatVietnamTime(new Date(row.created_at)) : row.created_at
        }));
        res.json(formattedRows);
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

        // Note: Do NOT update total_amount here because it was already calculated correctly when booking was created
        // total_amount already includes coin discount from createBooking endpoint
        console.log("⚠️  Skipping booking total_amount update: already applied at booking creation time");

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

const express = require("express");
const router = express.Router();
const db = require("../db");

router.get("/promotions", async (req, res) => {
  const nowSql = `
    SELECT id, code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status
    FROM promotions
    WHERE status = 'active'
      AND start_date <= NOW()
      AND end_date >= NOW()
    ORDER BY start_date DESC, id DESC
  `;
  try {
    const { rows } = await db.query(nowSql);
    res.json(rows);
  } catch (err) {
    console.error("Failed to fetch promotions:", err);
    res.status(500).json({ message: "Failed to fetch promotions" });
  }
});

router.get("/promotions/featured", async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit || "5", 10), 20) || 5;
  const sql = `
    SELECT id, code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status
    FROM promotions
    WHERE status = 'active'
      AND start_date <= NOW()
      AND end_date >= NOW()
    ORDER BY start_date DESC, id DESC
    LIMIT $1
  `;
  try {
    const { rows } = await db.query(sql, [limit]);
    res.json(rows);
  } catch (err) {
    console.error("Failed to fetch featured promotions:", err);
    res.status(500).json({ message: "Failed to fetch featured promotions" });
  }
});

// New: validate promo code and calculate discount
router.post('/promotions/validate', async (req, res) => {
  const { code, amount } = req.body;
  if (!code || typeof code !== 'string') return res.status(400).json({ message: 'Missing promotion code' });
  const amountNum = Number(amount || 0);
  if (isNaN(amountNum) || amountNum < 0) return res.status(400).json({ message: 'Invalid amount' });

  const sql = `
    SELECT id, code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status
    FROM promotions
    WHERE code = $1
    LIMIT 1
  `;
  try {
    const { rows } = await db.query(sql, [code]);
    if (!rows || rows.length === 0) {
      return res.json({ valid: false, promotion: null, discount: 0, final_amount: amountNum, reason: 'Mã không tồn tại' });
    }

    const promo = rows[0];
    // Check status and date window
    if (promo.status !== 'active') {
      return res.json({ valid: false, promotion: promo, discount: 0, final_amount: amountNum, reason: 'Mã không còn hiệu lực' });
    }
    const nowRes = await db.query('SELECT NOW() as now');
    const now = nowRes.rows && nowRes.rows[0] && nowRes.rows[0].now ? new Date(nowRes.rows[0].now) : new Date();
    if (promo.start_date && new Date(promo.start_date) > now) {
      return res.json({ valid: false, promotion: promo, discount: 0, final_amount: amountNum, reason: 'Mã chưa có hiệu lực' });
    }
    if (promo.end_date && new Date(promo.end_date) < now) {
      return res.json({ valid: false, promotion: promo, discount: 0, final_amount: amountNum, reason: 'Mã đã hết hạn' });
    }

    // Check min_price
    const minPrice = Number(promo.min_price || 0);
    if (minPrice > 0 && amountNum < minPrice) {
      return res.json({ valid: false, promotion: promo, discount: 0, final_amount: amountNum, reason: `Đơn hàng phải >= ${minPrice}` });
    }

    // Compute discount
    const type = (promo.discount_type || '').toLowerCase();
    const value = Number(promo.discount_value || 0);
    let rawDiscount = 0;
    if (type === 'percent' || type === 'percentage') {
      rawDiscount = amountNum * (value / 100);
    } else {
      // assume fixed amount
      rawDiscount = value;
    }

    const maxDiscount = Number(promo.max_discount || 0);
    let discount = rawDiscount;
    if (maxDiscount > 0) discount = Math.min(discount, maxDiscount);

    // Ensure discount not exceeding amount
    discount = Math.max(0, Math.min(discount, amountNum));
    const finalAmount = Math.max(0, Number((amountNum - discount).toFixed(2)));

    return res.json({ valid: true, promotion: promo, discount: Number(discount.toFixed(2)), final_amount: finalAmount });

  } catch (err) {
    console.error('Failed to validate promotion:', err);
    res.status(500).json({ message: 'Failed to validate promotion' });
  }
});

module.exports = router;

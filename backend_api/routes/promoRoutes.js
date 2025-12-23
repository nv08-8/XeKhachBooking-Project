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
    // Convert numeric strings to actual numbers for mobile app compatibility
    const converted = rows.map(row => ({
      ...row,
      discount_value: row.discount_value ? Number(row.discount_value) : 0,
      min_price: row.min_price ? Number(row.min_price) : 0,
      max_discount: row.max_discount ? Number(row.max_discount) : null
    }));
    res.json(converted);
  } catch (err) {
    console.error("Failed to fetch promotions:", err.stack || err);
    res.status(500).json({ message: "Failed to fetch promotions", error: (err && err.message) || String(err) });
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
    // Convert numeric strings to actual numbers for mobile app compatibility
    const converted = rows.map(row => ({
      ...row,
      discount_value: row.discount_value ? Number(row.discount_value) : 0,
      min_price: row.min_price ? Number(row.min_price) : 0,
      max_discount: row.max_discount ? Number(row.max_discount) : null
    }));
    res.json(converted);
  } catch (err) {
    console.error("Failed to fetch featured promotions:", err);
    res.status(500).json({ message: "Failed to fetch featured promotions" });
  }
});

// New: validate promo code and calculate discount
router.post('/promotions/validate', async (req, res) => {
  const { code, amount } = req.body;
  console.debug('PROMO validate payload:', { code, amount });

  // parse amount robustly: accept numbers or formatted strings like "183.000" or "183.000 đ"
  const parseAmount = (a) => {
    if (typeof a === 'number') return a;
    if (!a) return 0;
    const s = String(a);
    // Remove any non-digit or minus sign characters (strip thousands separators, currency symbols, spaces)
    const cleaned = s.replace(/[^\d-]/g, '');
    const n = Number(cleaned);
    return isNaN(n) ? 0 : n;
  };

  if (!code || typeof code !== 'string') return res.status(400).json({ message: 'Missing promotion code' });
  const amountNum = parseAmount(amount || 0);
  if (isNaN(amountNum) || amountNum < 0) return res.status(400).json({ message: 'Invalid amount' });

  const sql = `
    SELECT id, code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status
    FROM promotions
    WHERE LOWER(code) = LOWER($1)
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
    console.error('Failed to validate promotion:', err.stack || err);
    res.status(500).json({ message: 'Failed to validate promotion', error: (err && err.message) || String(err) });
  }
});

module.exports = router;

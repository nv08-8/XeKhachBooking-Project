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

module.exports = router;

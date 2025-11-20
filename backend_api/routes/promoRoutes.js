const express = require("express");
const router = express.Router();
const db = require("../db");

router.get("/promotions", async (req, res) => {
  const nowSql = `
    SELECT id, title, description, discount_percent, starts_at, ends_at, active, created_at
    FROM promotions
    WHERE active = TRUE
      AND (starts_at IS NULL OR starts_at <= NOW())
      AND (ends_at IS NULL OR ends_at >= NOW())
    ORDER BY starts_at DESC, id DESC
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
    SELECT id, title, description, discount_percent, starts_at, ends_at, active
    FROM promotions
    WHERE active = TRUE
      AND (starts_at IS NULL OR starts_at <= NOW())
      AND (ends_at IS NULL OR ends_at >= NOW())
    ORDER BY starts_at DESC, id DESC
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

router.get("/promos", async (req, res) => {
  const sql = `
    SELECT id, title, description, discount_percent, code, valid_until
    FROM promos
    WHERE valid_until >= CURRENT_DATE
    ORDER BY discount_percent DESC
    LIMIT 10
  `;
  try {
    const { rows } = await db.query(sql);
    res.json(rows || []);
  } catch (err) {
    console.error("Failed to fetch promos:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch promos" });
  }
});

module.exports = router;

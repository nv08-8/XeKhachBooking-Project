const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/promotions - list active promotions (current date within window)
router.get("/promotions", (req, res) => {
  const nowSql = `
    SELECT id, title, description, discount_percent, starts_at, ends_at, active, created_at
    FROM promotions
    WHERE active = 1
      AND (starts_at IS NULL OR starts_at <= NOW())
      AND (ends_at IS NULL OR ends_at >= NOW())
    ORDER BY starts_at DESC, id DESC
  `;
  db.query(nowSql, [], (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    res.json(rows);
  });
});

// GET /api/promotions/featured - top N promotions
router.get("/promotions/featured", (req, res) => {
  const limit = Math.min(parseInt(req.query.limit || "5", 10), 20) || 5;
  const sql = `
    SELECT id, title, description, discount_percent, starts_at, ends_at, active
    FROM promotions
    WHERE active = 1
      AND (starts_at IS NULL OR starts_at <= NOW())
      AND (ends_at IS NULL OR ends_at >= NOW())
    ORDER BY starts_at DESC, id DESC
    LIMIT ?
  `;
  db.query(sql, [limit], (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    res.json(rows);
  });
});

// GET /api/promos - simple promos with discount codes (from promos table)
router.get("/promos", (req, res) => {
  const sql = `
    SELECT id, title, description, discount_percent, code, valid_until
    FROM promos
    WHERE valid_until >= CURDATE()
    ORDER BY discount_percent DESC
    LIMIT 10
  `;
  db.query(sql, (err, rows) => {
    if (err) {
      console.error("Failed to fetch promos:", err.message || err);
      return res.status(500).json({ error: "Failed to fetch promos" });
    }
    res.json(rows || []);
  });
});

module.exports = router;

// backend_api/routes/promoRoutes.js
module.exports = router;

});
  });
    res.json(rows);
    if (err) return res.status(500).json({ message: err.message });
  db.query(sql, [limit], (err, rows) => {
  `;
    LIMIT ?
    ORDER BY starts_at DESC, id DESC
      AND (ends_at IS NULL OR ends_at >= NOW())
      AND (starts_at IS NULL OR starts_at <= NOW())
    WHERE active = 1
    FROM promotions
    SELECT id, title, description, discount_percent, starts_at, ends_at, active
  const sql = `
  const limit = Math.min(parseInt(req.query.limit || "5", 10), 20) || 5;
router.get("/promotions/featured", (req, res) => {
// GET /api/promotions/featured - top N promotions

});
  });
    res.json(rows);
    if (err) return res.status(500).json({ message: err.message });
  db.query(nowSql, [], (err, rows) => {
  `;
    ORDER BY starts_at DESC, id DESC
      AND (ends_at IS NULL OR ends_at >= NOW())
      AND (starts_at IS NULL OR starts_at <= NOW())
    WHERE active = 1
    FROM promotions
    SELECT id, title, description, discount_percent, starts_at, ends_at, active, created_at
  const nowSql = `
router.get("/promotions", (req, res) => {
// GET /api/promotions - list active promotions (current date within window)

const db = require("../db");
const router = express.Router();
const express = require("express");


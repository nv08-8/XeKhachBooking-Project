// backend_api/routes/dataRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/routes?origin=&destination=&q=
router.get("/routes", (req, res) => {
  const { origin, destination, q } = req.query;

  let sql = "SELECT id, origin, destination, distance_km, duration_min, created_at FROM routes WHERE 1=1";
  const params = [];

  if (origin) {
    sql += " AND origin LIKE ?";
    params.push(`%${origin}%`);
  }
  if (destination) {
    sql += " AND destination LIKE ?";
    params.push(`%${destination}%`);
  }
  if (q) {
    sql += " AND (origin LIKE ? OR destination LIKE ?)";
    params.push(`%${q}%`, `%${q}%`);
  }

  sql += " ORDER BY origin, destination";

  db.query(sql, params, (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    return res.json(rows);
  });
});

// GET /api/trips/:id/seats?available=true
router.get("/trips/:id/seats", (req, res) => {
  const tripId = parseInt(req.params.id, 10);
  const { available } = req.query;

  if (Number.isNaN(tripId)) return res.status(400).json({ message: "Invalid trip id" });

  let sql = "SELECT id, trip_id, label, type, is_booked, booking_id FROM seats WHERE trip_id=?";
  const params = [tripId];

  if (available === "true") {
    sql += " AND is_booked = 0";
  }

  sql += " ORDER BY label";

  db.query(sql, params, (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    return res.json(rows);
  });
});

module.exports = router;

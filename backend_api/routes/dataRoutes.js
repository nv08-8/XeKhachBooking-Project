// backend_api/routes/dataRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/routes?origin=&destination=&q=
router.get("/routes", async (req, res) => {
  const { origin, destination, q } = req.query;
  let sql = "SELECT id, origin, destination, distance_km, duration_min, created_at FROM routes WHERE 1=1";
  const params = [];

  if (origin) {
    sql += " AND origin ILIKE $" + (params.length + 1);
    params.push(`%${origin}%`);
  }
  if (destination) {
    sql += " AND destination ILIKE $" + (params.length + 1);
    params.push(`%${destination}%`);
  }
  if (q) {
    const idx1 = params.length + 1;
    const idx2 = params.length + 2;
    sql += ` AND (origin ILIKE $${idx1} OR destination ILIKE $${idx2})`;
    params.push(`%${q}%`, `%${q}%`);
  }

  sql += " ORDER BY origin, destination";

  try {
    const { rows } = await db.query(sql, params);
    return res.json(rows);
  } catch (err) {
    console.error("Lỗi lấy route:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});

// GET /api/trips/:id/seats?available=true
router.get("/trips/:id/seats", async (req, res) => {
  const tripId = parseInt(req.params.id, 10);
  const { available } = req.query;

  if (Number.isNaN(tripId)) return res.status(400).json({ message: "Invalid trip id" });

  let sql = "SELECT id, trip_id, label, type, is_booked, booking_id FROM seats WHERE trip_id=$1";
  const params = [tripId];

  if (available === "true") {
    sql += " AND is_booked = 0";
  }

  // Sắp xếp thông minh: A1 -> A2 -> ... -> A10
  sql += " ORDER BY LENGTH(label), label";

  try {
    const { rows } = await db.query(sql, params);

    // QUAN TRỌNG: Map dữ liệu để Android dễ xử lý
    // Chuyển is_booked từ 0/1 sang false/true
    const formattedRows = rows.map(seat => ({
        ...seat,
        is_booked: seat.is_booked === 1 || seat.is_booked === true // Đảm bảo luôn là boolean
    }));

    return res.json(formattedRows);
  } catch (err) {
    console.error("Lỗi lấy ghế:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});

module.exports = router;

const express = require("express");
const router = express.Router();
const db = require("../db");

/* ============================================================
    1. GET TRIPS (with optional search)
       - GET /api/trips
       - Query: route_id, origin, destination, date, status, bus_type, page, page_size
   ============================================================ */

router.get("/trips", (req, res) => {
  const { route_id, origin, destination, date, status, bus_type } = req.query;
  const page = Math.max(parseInt(req.query.page || "1", 10), 1);
  const pageSize = Math.min(Math.max(parseInt(req.query.page_size || "50", 10), 1), 200);
  const offset = (page - 1) * pageSize;

  let sql = `
    SELECT
      t.id, t.route_id, t.operator, t.bus_type, t.departure_time, t.arrival_time,
      t.price, t.seats_total, t.seats_available, t.status, t.created_at,
      r.origin, r.destination, r.distance_km, r.duration_min
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    WHERE 1=1`;
  const params = [];

  if (route_id) { sql += " AND t.route_id = ?"; params.push(route_id); }
  if (origin) { sql += " AND r.origin LIKE ?"; params.push(`%${origin}%`); }
  if (destination) { sql += " AND r.destination LIKE ?"; params.push(`%${destination}%`); }
  if (date) {
    sql += " AND t.departure_time >= ? AND t.departure_time < DATE_ADD(?, INTERVAL 1 DAY)";
    params.push(date, date);
  }
  if (status) { sql += " AND t.status = ?"; params.push(status); }
  if (bus_type) { sql += " AND t.bus_type LIKE ?"; params.push(`%${bus_type}%`); }

  sql += " ORDER BY t.departure_time ASC LIMIT ? OFFSET ?";
  params.push(pageSize, offset);

  db.query(sql, params, (err, rows) => {
    if (err) {
      console.error("Lỗi khi truy vấn danh sách chuyến xe:", err);
      return res.status(500).json({ message: "Lỗi phía server." });
    }
    res.json(rows);
  });
});

module.exports = router;

const express = require("express");
const router = express.Router();
const db = require("../db");

/* ============================================================
    1. GET TRIPS (with optional search)
       - GET /api/trips
       - Query: route_id, origin, destination, date, status, bus_type, page, page_size
   ============================================================ */

router.get("/trips", async (req, res) => {
  const { route_id, origin, destination, date, status, bus_type } = req.query;
  const page = Math.max(parseInt(req.query.page || "1", 10), 1);
  const pageSize = Math.min(Math.max(parseInt(req.query.page_size || "50", 10), 1), 200);
  const offset = (page - 1) * pageSize;

  let sql = `
    SELECT
      t.id, t.route_id, t.operator, t.bus_type, t.departure_time, t.arrival_time,
      t.price, t.seats_total, t.seats_available, t.status, t.created_at,
      r.origin, r.destination, r.distance_km, r.duration_min,
      b.number_plate, b.image_url AS bus_image_url,
      d.name AS driver_name, d.phone AS driver_phone
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    LEFT JOIN buses b ON b.id = t.bus_id
    LEFT JOIN drivers d ON d.id = t.driver_id
    WHERE 1=1`;
  const params = [];

  if (route_id) { sql += " AND t.route_id = $" + (params.length + 1); params.push(route_id); }
  if (origin) { sql += " AND r.origin ILIKE $" + (params.length + 1); params.push(`%${origin}%`); }
  if (destination) { sql += " AND r.destination ILIKE $" + (params.length + 1); params.push(`%${destination}%`); }
  if (date) {
    const startIdx = params.length + 1;
    const endIdx = params.length + 2;
    sql += ` AND t.departure_time >= $${startIdx} AND t.departure_time < $${endIdx}`;
    params.push(new Date(`${date}T00:00:00.000Z`), new Date(`${date}T23:59:59.999Z`));
  }
  if (status) { sql += " AND t.status = $" + (params.length + 1); params.push(status); }
  if (bus_type) { sql += " AND t.bus_type ILIKE $" + (params.length + 1); params.push(`%${bus_type}%`); }

  sql += " ORDER BY t.departure_time ASC LIMIT $" + (params.length + 1) + " OFFSET $" + (params.length + 2);
  params.push(pageSize, offset);

  try {
    const { rows } = await db.query(sql, params);
    res.json(rows);
  } catch (err) {
    console.error("Lỗi khi truy vấn danh sách chuyến xe:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});

module.exports = router;

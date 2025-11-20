// backend_api/routes/metaRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

router.get("/meta/locations", async (req, res) => {
  try {
    const originsPromise = db.query("SELECT DISTINCT origin AS name FROM routes ORDER BY origin");
    const destinationsPromise = db.query("SELECT DISTINCT destination AS name FROM routes ORDER BY destination");
    const [origins, destinations] = await Promise.all([originsPromise, destinationsPromise]);
    res.json({
      origins: origins.rows.map(o => o.name),
      destinations: destinations.rows.map(d => d.name)
    });
  } catch (err) {
    console.error("Failed fetching meta locations:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});

router.get("/popular", async (req, res) => {
  const aggQuery = `
    SELECT
      CONCAT(r.origin, ' → ', r.destination) AS name,
      r.origin AS start_location,
      r.destination AS end_location,
      SUM(t.seats_total - t.seats_available) AS seats_booked,
      ROUND(AVG(t.price)::numeric, 2) AS avg_price,
      COUNT(DISTINCT t.id) AS trip_count,
      r.distance_km,
      r.duration_min
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    GROUP BY r.origin, r.destination, r.distance_km, r.duration_min
    HAVING SUM(t.seats_total - t.seats_available) > 0
    ORDER BY seats_booked DESC
    LIMIT 10;
  `;
  try {
    const { rows } = await db.query(aggQuery);
    res.json(rows || []);
  } catch (err) {
    console.error("Failed computing popular routes:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch popular routes" });
  }
});

router.get("/reviews", async (req, res) => {
  const query = "SELECT * FROM reviews ORDER BY created_at DESC LIMIT 10";
  try {
    const { rows } = await db.query(query);
    res.json(rows || []);
  } catch (err) {
    console.error("Failed to fetch reviews:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch reviews" });
  }
});

router.get('/routes', async (req, res) => {
  const { origin, destination } = req.query;
  let sql = `SELECT id, origin, destination, distance_km, duration_min, created_at FROM routes WHERE 1=1`;
  const params = [];
  if (origin) { sql += ` AND origin = $${params.length + 1}`; params.push(origin); }
  if (destination) { sql += ` AND destination = $${params.length + 1}`; params.push(destination); }
  sql += ' ORDER BY id ASC';
  try {
    const { rows } = await db.query(sql, params);
    res.json(rows);
  } catch (err) {
    console.error("Failed to fetch routes:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch routes" });
  }
});

module.exports = router;

// backend_api/routes/metaRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/meta/locations - distinct origins and destinations for UI dropdowns
router.get("/meta/locations", (req, res) => {
  const queries = {
    origins: "SELECT DISTINCT origin AS name FROM routes ORDER BY origin",
    destinations: "SELECT DISTINCT destination AS name FROM routes ORDER BY destination",
  };

  db.query(queries.origins, [], (err1, origins) => {
    if (err1) return res.status(500).json({ message: err1.message });
    db.query(queries.destinations, [], (err2, destinations) => {
      if (err2) return res.status(500).json({ message: err2.message });
      res.json({ origins: origins.map(o => o.name), destinations: destinations.map(d => d.name) });
    });
  });
});

// GET /api/popular - compute popular routes by seats booked (seats_total - seats_available)
router.get("/popular", (req, res) => {
  const aggQuery = `
    SELECT
      CONCAT(r.origin, ' → ', r.destination) AS name,
      r.origin AS start_location,
      r.destination AS end_location,
      SUM(t.seats_total - t.seats_available) AS seats_booked,
      ROUND(AVG(t.price), 2) AS avg_price,
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

  db.query(aggQuery, (err, rows) => {
    if (err) {
      console.error("Failed computing popular routes:", err.message || err);
      return res.status(500).json({ error: "Failed to fetch popular routes" });
    }
    res.json(rows || []);
  });
});

// GET /api/reviews - get customer reviews
router.get("/reviews", (req, res) => {
  const query = "SELECT * FROM reviews ORDER BY created_at DESC LIMIT 10";
  db.query(query, (err, rows) => {
    if (err) {
      console.error("Failed to fetch reviews:", err.message || err);
      return res.status(500).json({ error: "Failed to fetch reviews" });
    }
    res.json(rows || []);
  });
});

// GET /api/routes - full list of routes (origin/destination in Vietnamese with diacritics)
router.get('/routes', (req, res) => {
  // Optional filters ?origin=...&destination=...
  const { origin, destination } = req.query;
  let sql = `SELECT id, origin, destination, distance_km, duration_min, created_at FROM routes WHERE 1=1`;
  const params = [];
  if (origin) { sql += ' AND origin = ?'; params.push(origin); }
  if (destination) { sql += ' AND destination = ?'; params.push(destination); }
  sql += ' ORDER BY id ASC';
  db.query(sql, params, (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(rows);
  });
});

module.exports = router;

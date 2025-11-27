// backend_api/routes/metaRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/meta/locations - distinct origins and destinations for UI dropdowns
router.get("/meta/locations", async (req, res) => {
  try {
    const { rows: origins } = await db.query("SELECT DISTINCT origin AS name FROM routes ORDER BY origin");
    const { rows: destinations } = await db.query("SELECT DISTINCT destination AS name FROM routes ORDER BY destination");
    res.json({ 
      origins: origins.map(o => o.name), 
      destinations: destinations.map(d => d.name) 
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// GET /api/meta/operators - distinct operators (nhà xe)
router.get('/meta/operators', async (req, res) => {
  try {
    const { rows } = await db.query("SELECT DISTINCT operator FROM trips WHERE operator IS NOT NULL ORDER BY operator");
    const operators = (rows || []).map(r => r.operator).filter(Boolean);
    res.json(operators);
  } catch (err) {
    console.error('Failed to fetch operators:', err.message || err);
    res.status(500).json({ message: 'Failed to fetch operators' });
  }
});

// GET /api/popular - compute popular routes by seats booked
router.get("/popular", async (req, res) => {
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
  try {
    const { rows } = await db.query(aggQuery);
    res.json(rows || []);
  } catch (err) {
    console.error("Failed computing popular routes:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch popular routes" });
  }
});

// GET /api/reviews - get recent customer reviews with user and trip info
router.get("/reviews", async (req, res) => {
  const query = `
    SELECT
        rev.id, rev.rating, rev.comment, rev.created_at,
        u.name AS user_name,
        r.origin, r.destination
    FROM reviews rev
    JOIN users u ON u.id = rev.user_id
    JOIN trips t ON t.id = rev.trip_id
    JOIN routes r ON r.id = t.route_id
    ORDER BY rev.created_at DESC
    LIMIT 10
  `;
  try {
    const { rows } = await db.query(query);
    res.json(rows || []);
  } catch (err) {
    console.error("Failed to fetch reviews:", err.message || err);
    return res.status(500).json({ error: "Failed to fetch reviews" });
  }
});

module.exports = router;

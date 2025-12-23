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
      r.duration_min,
      r.id as route_id
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    GROUP BY r.id, r.origin, r.destination, r.distance_km, r.duration_min
    HAVING SUM(t.seats_total - t.seats_available) > 0
    ORDER BY seats_booked DESC
    LIMIT 10;
  `;
  try {
    const { rows } = await db.query(aggQuery);

    // For each route, get a sample trip to extract operator and bus_type for image
    const routesWithImages = await Promise.all(rows.map(async (row) => {
      const sampleTripQuery = `
        SELECT operator, bus_type
        FROM trips
        WHERE route_id = $1
        LIMIT 1
      `;
      const { rows: tripRows } = await db.query(sampleTripQuery, [row.route_id]);
      const sampleTrip = tripRows && tripRows[0];

      return {
        ...row,
        seats_booked: row.seats_booked ? Number(row.seats_booked) : 0,
        avg_price: row.avg_price ? Number(row.avg_price) : 0,
        trip_count: row.trip_count ? Number(row.trip_count) : 0,
        distance_km: row.distance_km ? Number(row.distance_km) : null,
        duration_min: row.duration_min ? Number(row.duration_min) : null,
        sample_operator: sampleTrip?.operator || null,
        sample_bus_type: sampleTrip?.bus_type || null
      };
    }));

    res.json(routesWithImages || []);
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

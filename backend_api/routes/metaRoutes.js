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

module.exports = router;


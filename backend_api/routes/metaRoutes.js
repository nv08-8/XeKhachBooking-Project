const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/meta/locations
router.get("/locations", (req, res) => {
    const originsQuery = "SELECT DISTINCT from_location FROM trips;";
    const destinationsQuery = "SELECT DISTINCT to_location FROM trips;";

    db.query(originsQuery, (err, originRows) => {
        if (err) {
            return res.status(500).json({ message: "Error fetching origins.", error: err });
        }

        db.query(destinationsQuery, (err, destRows) => {
            if (err) {
                return res.status(500).json({ message: "Error fetching destinations.", error: err });
            }

            const origins = originRows.map(row => row.from_location);
            const destinations = destRows.map(row => row.to_location);

            res.json({
                origins,
                destinations
            });
        });
    });
});

module.exports = router;

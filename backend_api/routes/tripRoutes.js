const express = require("express");
const router = express.Router();
const db = require("../db");

/* ============================================================
    1. GET TRIPS (with optional search)
       - GET /api/trips
       - GET /api/trips?from=...&to=...
   ============================================================ */

router.get("/", (req, res) => {
    const { from, to } = req.query;

    let sql = "SELECT * FROM trips WHERE 1=1";
    const params = [];

    if (from) {
        sql += " AND from_location LIKE ?";
        params.push(`%${from}%`);
    }

    if (to) {
        sql += " AND to_location LIKE ?";
        params.push(`%${to}%`);
    }

    db.query(sql, params, (err, rows) => {
        if (err) {
            console.error("Lỗi khi truy vấn danh sách tuyến xe:", err);
            return res.status(500).json({ message: "Lỗi phía server." });
        }
        res.json(rows);
    });
});

module.exports = router;

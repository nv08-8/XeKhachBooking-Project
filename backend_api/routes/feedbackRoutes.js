const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /feedbacks/pending - Get bookings that are completed but not yet reviewed
router.get("/feedbacks/pending", async (req, res) => {
    const { user_id } = req.query;
    if (!user_id) return res.status(400).json({ message: "Missing user_id" });

    const sql = `
        SELECT b.id, b.trip_id, b.total_amount, b.created_at as booking_date,
               t.departure_time, t.operator, t.bus_type,
               r.origin, r.destination,
               COALESCE(array_agg(bi.seat_code), ARRAY[]::text[]) as seat_labels
        FROM bookings b
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        LEFT JOIN booking_items bi ON bi.booking_id = b.id
        LEFT JOIN feedbacks f ON f.booking_id = b.id
        WHERE b.user_id = $1 AND b.status = 'completed' AND f.id IS NULL
        GROUP BY b.id, t.id, r.id
        ORDER BY t.departure_time DESC
    `;

    try {
        const { rows } = await db.query(sql, [user_id]);
        res.json(rows);
    } catch (err) {
        console.error("Error fetching pending feedbacks:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// GET /feedbacks/reviewed - Get bookings that have been reviewed
router.get("/feedbacks/reviewed", async (req, res) => {
    const { user_id } = req.query;
    if (!user_id) return res.status(400).json({ message: "Missing user_id" });

    const sql = `
        SELECT f.id as feedback_id, f.rating, f.comment, f.created_at as feedback_date,
               b.id as booking_id, b.total_amount,
               t.departure_time, t.operator,
               r.origin, r.destination
        FROM feedbacks f
        JOIN bookings b ON f.booking_id = b.id
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        WHERE f.user_id = $1
        ORDER BY f.created_at DESC
    `;

    try {
        const { rows } = await db.query(sql, [user_id]);
        res.json(rows);
    } catch (err) {
        console.error("Error fetching reviewed feedbacks:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// POST /feedbacks - Submit a new feedback
router.post("/feedbacks", async (req, res) => {
    const { user_id, booking_id, rating, comment } = req.body;

    if (!user_id || !booking_id || !rating) {
        return res.status(400).json({ message: "Missing required fields" });
    }

    try {
        // Verify booking belongs to user and is completed
        const bookingCheck = await db.query(
            "SELECT id FROM bookings WHERE id = $1 AND user_id = $2 AND status = 'completed'",
            [booking_id, user_id]
        );

        if (bookingCheck.rowCount === 0) {
            return res.status(403).json({ message: "Invalid booking or booking not completed" });
        }

        // Insert feedback
        const result = await db.query(
            "INSERT INTO feedbacks (user_id, booking_id, rating, comment) VALUES ($1, $2, $3, $4) RETURNING *",
            [user_id, booking_id, rating, comment]
        );

        res.status(201).json(result.rows[0]);
    } catch (err) {
        if (err.code === '23505') { // Unique constraint violation (one feedback per booking)
            return res.status(409).json({ message: "Feedback already exists for this booking" });
        }
        console.error("Error submitting feedback:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

module.exports = router;

const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /feedbacks/pending - Get bookings that are completed (or confirmed and past departure) but not yet reviewed
router.get("/feedbacks/pending", async (req, res) => {
    const { user_id } = req.query;
    if (!user_id) return res.status(400).json({ message: "Missing user_id" });

    const sql = `
        SELECT b.id as booking_id, b.trip_id, b.total_amount, b.created_at as booking_date,
               t.id as trip_id, t.departure_time, t.arrival_time, t.operator, t.bus_type,
               r.origin, r.destination,
               CAST(b.total_amount AS VARCHAR) || ' VND' as price,
               CAST(EXTRACT(HOUR FROM (t.arrival_time - t.departure_time)) AS INT) || ' giờ' as duration,
               TO_CHAR(t.departure_time, 'Dy, DD Mon') as date,
               COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) as seat_labels
        FROM bookings b
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        LEFT JOIN booking_items bi ON bi.booking_id = b.id
        LEFT JOIN feedbacks f ON f.booking_id = b.id
        WHERE b.user_id = $1 
          AND (b.status = 'completed' OR (b.status = 'confirmed' AND (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()))
          AND f.id IS NULL
        GROUP BY b.id, t.id, r.id, t.departure_time, t.arrival_time, t.operator, t.bus_type, r.origin, r.destination, b.total_amount
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
               t.departure_time, t.arrival_time, t.operator, t.bus_type,
               r.origin, r.destination,
               u.name as user_name,
               CAST(b.total_amount AS VARCHAR) || ' VND' as price,
               CAST(EXTRACT(HOUR FROM (t.arrival_time - t.departure_time)) AS INT) || ' giờ' as duration,
               TO_CHAR(t.departure_time, 'Dy, DD Mon') as date
        FROM feedbacks f
        JOIN bookings b ON f.booking_id = b.id
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        JOIN users u ON f.user_id = u.id
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
        // Verify booking belongs to user and is completed or confirmed (past departure)
        const bookingCheck = await db.query(
            `SELECT b.id FROM bookings b 
             JOIN trips t ON b.trip_id = t.id
             WHERE b.id = $1 AND b.user_id = $2 
             AND (b.status = 'completed' OR (b.status = 'confirmed' AND (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()))`,
            [booking_id, user_id]
        );

        if (bookingCheck.rowCount === 0) {
            return res.status(403).json({ message: "Invalid booking or trip has not departed yet" });
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

// ✅ PUT /feedbacks/{id} - Update feedback
router.put("/feedbacks/:id", async (req, res) => {
    const { id } = req.params;
    const { comment } = req.body;

    if (!comment) {
        return res.status(400).json({ message: "Missing comment" });
    }

    try {
        const result = await db.query(
            "UPDATE feedbacks SET comment = $1, updated_at = NOW() WHERE id = $2 RETURNING *",
            [comment, id]
        );

        if (result.rowCount === 0) {
            return res.status(404).json({ message: "Feedback not found" });
        }

        res.json(result.rows[0]);
    } catch (err) {
        console.error("Error updating feedback:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// ✅ DELETE /feedbacks/{id} - Delete feedback
router.delete("/feedbacks/:id", async (req, res) => {
    const { id } = req.params;

    try {
        const result = await db.query(
            "DELETE FROM feedbacks WHERE id = $1",
            [id]
        );

        if (result.rowCount === 0) {
            return res.status(404).json({ message: "Feedback not found" });
        }

        res.status(204).send();
    } catch (err) {
        console.error("Error deleting feedback:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// ✅ POST /feedbacks/{id}/reply - Reply to feedback
router.post("/feedbacks/:id/reply", async (req, res) => {
    const { id } = req.params;
    const { reply } = req.body;

    if (!reply) {
        return res.status(400).json({ message: "Missing reply content" });
    }

    try {
        const result = await db.query(
            "UPDATE feedbacks SET reply = $1, reply_date = NOW() WHERE id = $2 RETURNING *",
            [reply, id]
        );

        if (result.rowCount === 0) {
            return res.status(404).json({ message: "Feedback not found" });
        }

        res.json(result.rows[0]);
    } catch (err) {
        console.error("Error replying to feedback:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// ✅ GET /api/admin/feedbacks - Admin lấy tất cả feedback từ tất cả người dùng
router.get("/admin/feedbacks", async (req, res) => {
    const sql = `
        SELECT f.id as feedback_id, f.rating, f.comment, f.created_at as feedback_date,
               b.id as booking_id, b.total_amount,
               t.departure_time, t.arrival_time, t.operator, t.bus_type,
               r.origin, r.destination,
               u.name as user_name,
               CAST(b.total_amount AS VARCHAR) || ' VND' as price,
               CAST(EXTRACT(HOUR FROM (t.arrival_time - t.departure_time)) AS INT) || ' giờ' as duration,
               TO_CHAR(t.departure_time, 'Dy, DD Mon') as date
        FROM feedbacks f
        JOIN bookings b ON f.booking_id = b.id
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        JOIN users u ON f.user_id = u.id
        ORDER BY f.created_at DESC
    `;

    try {
        const { rows } = await db.query(sql);
        res.json(rows);
    } catch (err) {
        console.error("Error fetching all feedbacks:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// ✅ GET /api/trips/{id}/feedbacks - Lấy feedback của một chuyến cụ thể
router.get("/trips/:id/feedbacks", async (req, res) => {
    const { id } = req.params;

    const sql = `
        SELECT f.id as feedback_id, f.rating, f.comment, f.created_at as feedback_date,
               u.name as user_name
        FROM feedbacks f
        JOIN bookings b ON f.booking_id = b.id
        JOIN users u ON f.user_id = u.id
        WHERE b.trip_id = $1
        ORDER BY f.created_at DESC
    `;

    try {
        const { rows } = await db.query(sql, [id]);
        res.json(rows);
    } catch (err) {
        console.error("Error fetching trip feedbacks:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

// ✅ GET /api/feedbacks/trips-with-feedback/{user_id} - Lấy các chuyến có feedback của user
router.get("/feedbacks/trips-with-feedback/:user_id", async (req, res) => {
    const { user_id } = req.params;

    const sql = `
        SELECT DISTINCT
               t.id,
               t.departure_time,
               t.arrival_time,
               t.operator,
               t.bus_type,
               r.origin,
               r.destination,
               COUNT(f.id) as feedback_count
        FROM bookings b
        INNER JOIN trips t ON b.trip_id = t.id
        INNER JOIN routes r ON t.route_id = r.id
        INNER JOIN feedbacks f ON f.booking_id = b.id
        WHERE b.user_id = $1
        GROUP BY t.id, t.departure_time, t.arrival_time, t.operator, t.bus_type, r.origin, r.destination
        ORDER BY t.departure_time DESC
    `;

    try {
        const { rows } = await db.query(sql, [user_id]);
        res.json(rows);
    } catch (err) {
        console.error("Error fetching trips with feedback:", err);
        res.status(500).json({ message: "Internal server error" });
    }
});

module.exports = router;

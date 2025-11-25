const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/trips/:id - Get trip details
router.get("/trips/:id", async (req, res) => {
    const tripId = req.params.id;

    try {
        const tripQuery = `
            SELECT
                t.id, t.route_id, t.operator, t.bus_type, t.departure_time, t.arrival_time,
                t.price, t.seats_total, t.seats_available, t.status,
                r.origin, r.destination, r.distance_km, r.duration_min,
                b.number_plate, b.image_url AS bus_image_url, b.seat_layout,
                d.name AS driver_name, d.phone AS driver_phone
            FROM trips t
            LEFT JOIN routes r ON t.route_id = r.id
            LEFT JOIN buses b ON t.bus_id = b.id
            LEFT JOIN drivers d ON t.driver_id = d.id
            WHERE t.id = $1
        `;

        const reviewsQuery = `
            SELECT r.rating, r.comment, u.name as user_name
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            WHERE r.trip_id = $1
            ORDER BY r.created_at DESC
        `;

        const stopsQuery = `
            WITH first_stop_time AS (
                SELECT estimate_time
                FROM route_stops
                WHERE route_id = (SELECT route_id FROM trips WHERE id = $1)
                ORDER BY order_index ASC
                LIMIT 1
            )
            SELECT
                rs.id, rs.name, rs.address, rs.type, rs.order_index,
                (t.departure_time + (rs.estimate_time - (SELECT estimate_time FROM first_stop_time)))::timestamp AS estimated_arrival_time
            FROM route_stops rs
            JOIN trips t ON t.route_id = rs.route_id
            WHERE t.id = $1
            ORDER BY rs.order_index ASC;
        `;

        const tripResult = await db.query(tripQuery, [tripId]);

        if (tripResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy chuyến đi"
            });
        }

        const trip = tripResult.rows[0];
        const reviewsResult = await db.query(reviewsQuery, [tripId]);
        const stopsResult = await db.query(stopsQuery, [tripId]);

        const amenities = getAmenitiesByBusType(trip.bus_type);
        const timeline = stopsResult.rows.map(stop => ({
            location: stop.name,
            address: stop.address,
            time: stop.estimated_arrival_time,
            type: stop.type,
            stop_id: stop.id,
            order: stop.order_index
        }));

        return res.json({
            success: true,
            data: {
                ...trip,
                amenities,
                timeline,
                reviews: reviewsResult.rows,
                departure_display: formatTime(trip.departure_time),
                arrival_display: formatTime(trip.arrival_time),
                duration_display: formatDuration(trip.duration_min)
            }
        });

    } catch (error) {
        console.error("Error fetching trip details:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi server khi lấy thông tin chuyến đi"
        });
    }
});

function getAmenitiesByBusType(busType) {
    const amenities = { wifi: false, water: false, ac: true, wc: false, tv: false, charging: false };
    if (!busType) return amenities;
    const type = busType.toLowerCase();
    if (type.includes("limousine")) Object.assign(amenities, { wifi: true, water: true, charging: true, tv: true });
    if (type.includes("wc")) amenities.wc = true;
    if (type.includes("limousine") || (type.match(/\d{2,}/)?.[0] || 0) >= 40) Object.assign(amenities, { wifi: true, water: true });
    return amenities;
}

function formatTime(isoString) {
    if (!isoString) return "N/A";
    return new Date(isoString).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

function formatDuration(minutes) {
    if (!minutes) return "N/A";
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h} giờ ${m > 0 ? m + " phút" : ""}`.trim();
}

module.exports = router;

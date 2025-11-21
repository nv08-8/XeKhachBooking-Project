const express = require("express");
const router = express.Router();
const db = require("../db");

// GET /api/trips/:id - Get trip details
router.get("/trips/:id", async (req, res) => {
    const tripId = req.params.id;

    try {
        const tripQuery = `
            SELECT
                t.id,
                t.route_id,
                t.operator,
                t.bus_type,
                t.departure_time,
                t.arrival_time,
                t.price,
                t.seats_total,
                t.seats_available,
                t.status,
                r.origin,
                r.destination,
                r.duration_hours,
                r.distance_km,
                r.pickup_point,
                r.dropoff_point
            FROM trips t
            LEFT JOIN routes r ON t.route_id = r.id
            WHERE t.id = $1
        `;

        const result = await db.query(tripQuery, [tripId]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy chuyến đi"
            });
        }

        const trip = result.rows[0];

        // Get amenities/facilities (from bus_type or default)
        const amenities = getAmenitiesByBusType(trip.bus_type);

        // Get route stops/timeline
        const timeline = generateTimeline(
            trip.origin,
            trip.destination,
            trip.departure_time,
            trip.arrival_time,
            trip.duration_hours
        );

        // Calculate rating (mock for now, you can add reviews table later)
        const rating = {
            average: 4.5,
            total_reviews: 1283,
            breakdown: {
                5: 50,
                4: 35,
                3: 10,
                2: 3,
                1: 2
            }
        };

        return res.json({
            success: true,
            data: {
                ...trip,
                amenities,
                timeline,
                rating,
                // Format times for display
                departure_display: formatTime(trip.departure_time),
                arrival_display: formatTime(trip.arrival_time),
                duration_display: formatDuration(trip.duration_hours)
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

// Helper function to get amenities based on bus type
function getAmenitiesByBusType(busType) {
    const amenities = {
        wifi: false,
        water: false,
        ac: false,
        wc: false,
        tv: false,
        charging: false
    };

    if (!busType) return amenities;

    const type = busType.toLowerCase();

    // All bus types have AC
    amenities.ac = true;

    // Limousine types have more amenities
    if (type.includes("limousine")) {
        amenities.wifi = true;
        amenities.water = true;
        amenities.charging = true;
        amenities.tv = true;
    }

    // Buses with WC
    if (type.includes("wc") || type.includes("có wc")) {
        amenities.wc = true;
    }

    // Higher-end buses (40+ seats or limousine)
    if (type.includes("limousine") || type.match(/\d{2,}/)?.[0] >= 40) {
        amenities.wifi = true;
        amenities.water = true;
    }

    return amenities;
}

// Generate timeline with stops
function generateTimeline(origin, destination, departureTime, arrivalTime, durationHours) {
    const timeline = [];

    // Start point
    timeline.push({
        location: `Bến xe ${origin}`,
        time: departureTime,
        type: "departure",
        description: "Điểm khởi hành"
    });

    // Middle stop (if journey is long enough)
    if (durationHours && durationHours > 4) {
        const midpointTime = new Date(new Date(departureTime).getTime() + (durationHours / 2 * 60 * 60 * 1000));
        timeline.push({
            location: "Trạm dừng chân Madagui",
            time: midpointTime.toISOString(),
            type: "rest_stop",
            description: "Nghỉ 30 phút"
        });
    }

    // End point
    timeline.push({
        location: `Bến xe ${destination}`,
        time: arrivalTime,
        type: "arrival",
        description: "Điểm đến"
    });

    return timeline;
}

// Format time to HH:mm
function formatTime(isoString) {
    if (!isoString) return "N/A";
    const date = new Date(isoString);
    return date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

// Format duration
function formatDuration(hours) {
    if (!hours) return "N/A";
    const h = Math.floor(hours);
    const m = Math.round((hours - h) * 60);
    return `${h} giờ ${m > 0 ? m + " phút" : ""}`.trim();
}

module.exports = router;


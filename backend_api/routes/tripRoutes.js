const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateDetailedSeatLayout } = require('../data/seat_layout.js');

/* ============================================================
    1. GET TRIPS (with optional search)
       - GET /api/trips
       - GET /api/trips/search (alias for /api/trips to fix Auth/Route issues)
       - Query: route_id, origin, destination, date, status, bus_type, page, page_size
   ============================================================ */

const getTrips = async (req, res) => {
  const { route_id, origin, destination, date, status, bus_type } = req.query;
  const page = Math.max(parseInt(req.query.page || "1", 10), 1);
  const pageSize = Math.min(Math.max(parseInt(req.query.page_size || "50", 10), 1), 200);
  const offset = (page - 1) * pageSize;

  console.log('\n🔍 [GET /api/trips] Query params:', { route_id, origin, destination, date, status, bus_type, page, pageSize });

  let sql = `
    SELECT
      t.id, t.route_id, t.operator, t.bus_type, t.departure_time, t.arrival_time,
      t.price, t.seats_total, t.seats_available, t.status, t.created_at,
      r.origin, r.destination, r.distance_km, r.duration_min,
      b.number_plate, b.image_url AS specific_bus_image,
      (SELECT image_urls FROM bus_images bi WHERE LOWER(TRIM(bi.bus_type)) = LOWER(TRIM(t.bus_type)) LIMIT 1) AS generic_bus_images,
      d.name AS driver_name, d.phone AS driver_phone
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    LEFT JOIN buses b ON b.id = t.bus_id
    LEFT JOIN drivers d ON d.id = t.driver_id
    WHERE 1=1`;
  const params = [];

  // Filter out past trips - only show trips that haven't departed yet
  sql += " AND t.departure_time > NOW()";

  if (route_id) { sql += " AND t.route_id = $" + (params.length + 1); params.push(route_id); }
  if (origin) { sql += " AND r.origin ILIKE $" + (params.length + 1); params.push(`%${origin}%`); }
  if (destination) { sql += " AND r.destination ILIKE $" + (params.length + 1); params.push(`%${destination}%`); }
  if (date) {
    const startIdx = params.length + 1;
    const endIdx = params.length + 2;
    sql += ` AND t.departure_time >= $${startIdx} AND t.departure_time < $${endIdx}`;
    params.push(new Date(`${date}T00:00:00.000Z`), new Date(`${date}T23:59:59.999Z`));
  }
  if (status) { sql += " AND t.status = $" + (params.length + 1); params.push(status); }
  if (bus_type) { sql += " AND t.bus_type ILIKE $" + (params.length + 1); params.push(`%${bus_type}%`); }

  sql += " ORDER BY t.departure_time ASC LIMIT $" + (params.length + 1) + " OFFSET $" + (params.length + 2);
  params.push(pageSize, offset);

  try {
    const { rows } = await db.query(sql, params);
    console.log(`✅ [GET /api/trips] Query returned ${rows.length} trips`);
    if (rows.length > 0) {
      console.log('   First trip:', { id: rows[0].id, route: `${rows[0].origin} → ${rows[0].destination}`, departure: rows[0].departure_time });
    }

    // Process images
    const results = rows.map(row => {
        let finalImageUrl = row.specific_bus_image;
        
        // If no specific image, try generic
        if (!finalImageUrl && row.generic_bus_images) {
            let images = [];
            try {
                if (typeof row.generic_bus_images === 'string') {
                    images = JSON.parse(row.generic_bus_images);
                } else if (Array.isArray(row.generic_bus_images)) {
                    images = row.generic_bus_images;
                }
            } catch (e) {
                // ignore parse error
            }
            
            // Filter valid non-TikTok images
            const valid = images.filter(url => 
                url && typeof url === 'string' && !url.includes('tiktok.com')
            );
            
            if (valid.length > 0) {
                finalImageUrl = valid[0];
            }
        }
        
        return {
            ...row,
            bus_image_url: finalImageUrl,
            specific_bus_image: undefined,
            generic_bus_images: undefined
        };
    });

    res.json(results);
  } catch (err) {
    console.error("Lỗi khi truy vấn danh sách chuyến xe:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
};

// Explicitly define /trips/search BEFORE /trips/:id to avoid ID conflict
// and to ensure both Guest and User can access it without middleware issues.
router.get("/trips/search", getTrips);
router.get("/trips", getTrips);

/* ============================================================
    2. GET TRIP DETAILS
       - GET /api/trips/:id
   ============================================================ */

router.get("/trips/:id", async (req, res) => {
    const tripId = req.params.id;

    try {
        const tripQuery = `
            SELECT
                t.id, t.route_id, t.operator, t.bus_type, t.departure_time, t.arrival_time,
                t.price, t.seats_total, t.seats_available, t.status,
                r.origin, r.destination, r.distance_km, r.duration_min,
                b.number_plate, b.image_url AS specific_bus_image, b.seat_layout,
                (SELECT image_urls FROM bus_images bi WHERE LOWER(TRIM(bi.bus_type)) = LOWER(TRIM(t.bus_type)) LIMIT 1) AS generic_bus_images,
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

        if (trip.seat_layout) {
            try {
                let seatLayoutObject;
                if (typeof trip.seat_layout === 'string') {
                    seatLayoutObject = JSON.parse(trip.seat_layout);
                } else {
                    seatLayoutObject = trip.seat_layout;
                }
                
                trip.seat_layout = generateDetailedSeatLayout(trip.bus_type, seatLayoutObject);
            } catch (e) {
                console.error("Lỗi khi xử lý seat_layout:", e);
            }
        }

        // Image fallback logic for detail view as well
        let finalImageUrl = trip.specific_bus_image;
        if (!finalImageUrl && trip.generic_bus_images) {
             let images = [];
            try {
                if (typeof trip.generic_bus_images === 'string') images = JSON.parse(trip.generic_bus_images);
                else if (Array.isArray(trip.generic_bus_images)) images = trip.generic_bus_images;
            } catch (e) {}
             const valid = images.filter(url => url && typeof url === 'string' && !url.includes('tiktok.com'));
             if (valid.length > 0) finalImageUrl = valid[0];
        }
        trip.bus_image_url = finalImageUrl;
        delete trip.specific_bus_image;
        delete trip.generic_bus_images;

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
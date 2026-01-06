const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateDetailedSeatLayout } = require('../data/seat_layout.js');

// Middleware to check for user_id header
const checkUserId = (req, res, next) => {
  const userId = req.headers['user-id'];
  if (!userId) {
    return res.status(401).json({ message: "Unauthorized: Missing user ID" });
  }
  req.userId = userId; // Attach userId to the request object
  next();
};


/* ============================================================
    1. GET TRIPS (with optional search)
       - GET /api/trips
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
      d.name AS driver_name, d.phone AS driver_phone,
      (SELECT COALESCE(ROUND(AVG(f2.rating)::numeric, 1), 0)
       FROM feedbacks f2
       JOIN bookings b2 ON f2.booking_id = b2.id
       JOIN trips t2 ON b2.trip_id = t2.id
       WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as operator_rating,
      (SELECT COALESCE(COUNT(f2.id), 0)
       FROM feedbacks f2
       JOIN bookings b2 ON f2.booking_id = b2.id
       JOIN trips t2 ON b2.trip_id = t2.id
       WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as total_ratings
    FROM trips t
    JOIN routes r ON r.id = t.route_id
    LEFT JOIN buses b ON b.id = t.bus_id
    LEFT JOIN drivers d ON d.id = t.driver_id
    WHERE 1=1`;
  const params = [];

  // Filter out past trips - only show trips that haven't departed yet
  // Also exclude cancelled trips from public view
  sql += " AND t.departure_time > NOW() AND t.status != 'cancelled'";

  if (route_id) { sql += " AND t.route_id = $" + (params.length + 1); params.push(route_id); }
  if (origin) { sql += " AND r.origin ILIKE $" + (params.length + 1); params.push(`%${origin}%`); }
  if (destination) { sql += " AND r.destination ILIKE $" + (params.length + 1); params.push(`%${destination}%`); }
  if (date) {
    const startIdx = params.length + 1;
    const endIdx = params.length + 2;
    // Removed 'Z' to avoid forcing UTC if server is in local time, but typically database stores in UTC
    sql += ` AND t.departure_time >= $${startIdx} AND t.departure_time < $${endIdx}`;
    params.push(new Date(`${date}T00:00:00`), new Date(`${date}T23:59:59`));
  }
  if (status) { sql += " AND t.status = $" + (params.length + 1); params.push(status); }
  if (bus_type) { sql += " AND t.bus_type ILIKE $" + (params.length + 1); params.push(`%${bus_type}%`); }

  sql += " ORDER BY t.departure_time ASC LIMIT $" + (params.length + 1) + " OFFSET $" + (params.length + 2);
  params.push(pageSize, offset);

  try {
    const { rows } = await db.query(sql, params);
    
    // Process images
    const results = rows.map(row => {
        let finalImageUrl = row.specific_bus_image;
        
        if (!finalImageUrl && row.generic_bus_images) {
            let images = [];
            try {
                if (typeof row.generic_bus_images === 'string') {
                    images = JSON.parse(row.generic_bus_images);
                } else if (Array.isArray(row.generic_bus_images)) {
                    images = row.generic_bus_images;
                }
            } catch (e) {}
            
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
            generic_bus_images: undefined,
            departure_time: row.departure_time ? formatLocalISO(new Date(row.departure_time)) : row.departure_time,
            arrival_time: row.arrival_time ? formatLocalISO(new Date(row.arrival_time)) : row.arrival_time,
            created_at: row.created_at ? formatLocalISO(new Date(row.created_at)) : row.created_at
        };
    });

    res.json(results);
  } catch (err) {
    console.error("Lỗi khi truy vấn danh sách chuyến xe:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
};

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
                d.name AS driver_name, d.phone AS driver_phone,
                (SELECT COALESCE(ROUND(AVG(f2.rating)::numeric, 1), 0)
                 FROM feedbacks f2
                 JOIN bookings b2 ON f2.booking_id = b2.id
                 JOIN trips t2 ON b2.trip_id = t2.id
                 WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as operator_rating,
                (SELECT COALESCE(COUNT(f2.id), 0)
                 FROM feedbacks f2
                 JOIN bookings b2 ON f2.booking_id = b2.id
                 JOIN trips t2 ON b2.trip_id = t2.id
                 WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as total_ratings
            FROM trips t
            LEFT JOIN routes r ON t.route_id = r.id
            LEFT JOIN buses b ON t.bus_id = b.id
            LEFT JOIN drivers d ON d.id = t.driver_id
            WHERE t.id = $1
        `;

        const reviewsQuery = `
            SELECT f.id as feedback_id, f.rating, f.comment, u.name as user_name, f.created_at
            FROM feedbacks f
            JOIN users u ON f.user_id = u.id
            JOIN bookings b ON f.booking_id = b.id
            JOIN trips t ON b.trip_id = t.id
            WHERE LOWER(TRIM(t.operator)) = LOWER(TRIM((SELECT operator FROM trips WHERE id = $1)))
            ORDER BY f.created_at DESC
            LIMIT 20
        `;

        const stopsQuery = `
            SELECT
                rs.id, rs.name, rs.address, rs.type, rs.order_index,
                (t.departure_time + (rs.travel_minutes_from_start * interval '1 minute')) AS estimated_arrival_time
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
            time: stop.estimated_arrival_time ? formatLocalISO(new Date(stop.estimated_arrival_time)) : stop.estimated_arrival_time,
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

        // Image fallback logic
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
            ...trip,
            amenities,
            timeline,
            reviews: reviewsResult.rows,
            departure_display: formatTime(trip.departure_time),
            arrival_display: formatTime(trip.arrival_time),
            duration_display: formatDuration(trip.duration_min)
        });

    } catch (error) {
        console.error("Error fetching trip details:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi server khi lấy thông tin chuyến đi"
        });
    }
});

/* ============================================================
    3. FAVORITES
       - GET /api/favorites
       - POST /api/favorites
       - DELETE /api/favorites/:trip_id
   ============================================================ */

router.get("/favorites", checkUserId, async (req, res) => {
  try {
    const sql = `
        SELECT t.*, r.origin, r.destination
        FROM favorites f
        JOIN trips t ON f.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        WHERE f.user_id = $1
        ORDER BY f.created_at DESC
    `;
    const { rows } = await db.query(sql, [req.userId]);
    res.json(rows);
  } catch (err) {
    console.error("Error fetching favorites:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách yêu thích" });
  }
});

router.post("/favorites", checkUserId, async (req, res) => {
  const { trip_id } = req.body;
  if (!trip_id) {
    return res.status(400).json({ message: "Thiếu trip_id" });
  }

  try {
    const sql = `
        INSERT INTO favorites (user_id, trip_id, created_at)
        VALUES ($1, $2, NOW())
        ON CONFLICT (user_id, trip_id) DO NOTHING
        RETURNING *
    `;
    const { rows } = await db.query(sql, [req.userId, trip_id]);
    if (rows.length === 0) {
        return res.status(200).json({ message: "Đã có trong danh sách yêu thích" });
    }
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error("Error adding favorite:", err);
    res.status(500).json({ message: "Lỗi khi thêm vào danh sách yêu thích" });
  }
});

router.delete("/favorites/:trip_id", checkUserId, async (req, res) => {
  const { trip_id } = req.params;
  if (!trip_id) {
    return res.status(400).json({ message: "Thiếu trip_id" });
  }

  try {
    const sql = `
        DELETE FROM favorites
        WHERE user_id = $1 AND trip_id = $2
        RETURNING id
    `;
    const { rowCount } = await db.query(sql, [req.userId, trip_id]);
    if (rowCount === 0) {
        return res.status(404).json({ message: "Không tìm thấy trong danh sách yêu thích" });
    }
    res.json({ message: "Xóa khỏi danh sách yêu thích thành công" });
  } catch (err) {
    console.error("Error deleting favorite:", err);
    res.status(500).json({ message: "Lỗi khi xóa khỏi danh sách yêu thích" });
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

// Helper to generate ISO-like timestamp without trailing Z (local time)
function formatLocalISO(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = date.getFullYear();
  const mm = pad(date.getMonth() + 1);
  const dd = pad(date.getDate());
  const hh = pad(date.getHours());
  const min = pad(date.getMinutes());
  const ss = pad(date.getSeconds());
  const ms = String(date.getMilliseconds()).padStart(3, '0');
  return `${yyyy}-${mm}-${dd}T${hh}:${min}:${ss}.${ms}`; // no trailing Z
}

module.exports = router;

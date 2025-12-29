// backend_api/routes/dataRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateDetailedSeatLayout } = require('../data/seat_layout.js');

// GET /api/routes?origin=&destination=&q=
router.get("/routes", async (req, res) => {
  const { origin, destination, q } = req.query;
  
  // Sửa câu truy vấn để đếm các chuyến sắp chạy (departure_time >= NOW())
  let sql = `
    SELECT 
      r.id, 
      r.origin, 
      r.destination, 
      r.distance_km, 
      r.duration_min, 
      r.created_at,
      COUNT(t.id) AS total_trip_count,
      COUNT(t.id) FILTER (WHERE t.departure_time >= NOW()) AS upcoming_trip_count
    FROM routes r
    LEFT JOIN trips t ON r.id = t.route_id
    WHERE 1=1
  `;

  const params = [];

  if (origin) {
    sql += " AND r.origin ILIKE $" + (params.length + 1);
    params.push(`%${origin}%`);
  }
  if (destination) {
    sql += " AND r.destination ILIKE $" + (params.length + 1);
    params.push(`%${destination}%`);
  }
  if (q) {
    const idx1 = params.length + 1;
    const idx2 = params.length + 2;
    sql += ` AND (r.origin ILIKE $${idx1} OR r.destination ILIKE $${idx2})`;
    params.push(`%${q}%`, `%${q}%`);
  }

  // Thêm GROUP BY để hàm COUNT hoạt động đúng
  sql += " GROUP BY r.id ORDER BY r.origin, r.destination";

  try {
    const { rows } = await db.query(sql, params);
    return res.json(rows);
  } catch (err) {
    console.error("Lỗi lấy route:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});

// GET /api/trips?route_id=&origin=&destination=&date=
router.get("/trips", async (req, res) => {
  const { route_id, origin, destination, date } = req.query;
  let sql = "SELECT * FROM trips WHERE 1=1";
  const params = [];

  if (route_id) {
    sql += " AND route_id = $" + (params.length + 1);
    params.push(parseInt(route_id, 10));
  }
  if (origin) {
    sql += " AND origin ILIKE $" + (params.length + 1);
    params.push(`%${origin}%`);
  }
  if (destination) {
    sql += " AND destination ILIKE $" + (params.length + 1);
    params.push(`%${destination}%`);
  }
  if (date) {
    sql += " AND DATE(departure_time) = $" + (params.length + 1);
    params.push(date);
  }

  sql += " ORDER BY departure_time DESC";

  try {
    const { rows } = await db.query(sql, params);
    return res.json(rows);
  } catch (err) {
    console.error("Lỗi lấy trip:", err);
    return res.status(500).json({ message: "Lỗi phía server." });
  }
});


// GET /api/trips/:id/seats?available=true
router.get("/trips/:id/seats", async (req, res) => {
    const tripId = parseInt(req.params.id, 10);
    const { available } = req.query;

    if (Number.isNaN(tripId)) {
        return res.status(400).json({ message: "Invalid trip id" });
    }

    try {
        // 1. Lấy thông tin chuyến đi để biết tổng số ghế và loại xe
        const tripRes = await db.query("SELECT seats_total, bus_type FROM trips WHERE id = $1", [tripId]);
        if (tripRes.rows.length === 0) {
            return res.status(404).json({ message: "Trip not found" });
        }
        const { seats_total, bus_type } = tripRes.rows[0];

        // 2. Lấy danh sách các ghế đã được đặt từ DB
        const bookedSeatsRes = await db.query("SELECT label FROM seats WHERE trip_id = $1 AND is_booked = TRUE", [tripId]);
        const bookedSeatLabels = new Set(bookedSeatsRes.rows.map(seat => seat.label));

        // 3. Tạo ra một sơ đồ ghế đầy đủ và hợp nhất trạng thái
        const fullSeatLayout = generateDetailedSeatLayout(bus_type, seats_total);
        const allSeats = fullSeatLayout.flat().map(seat => {
            if (seat.type === 'aisle' || !seat.label) {
                return {
                    label: seat.label || '',
                    seatType: 'aisle',
                    isBooked: false,
                };
            }

            const isBooked = bookedSeatLabels.has(seat.label);
            return {
                trip_id: tripId,
                label: seat.label,
                seatType: seat.type || 'seat',
                isBooked: isBooked,
            };
        });
        
        // 4. Lọc và trả về kết quả
        let finalSeats = allSeats;
        if (available === 'true') {
            // Chỉ trả về ghế khả dụng (chưa được đặt và không phải lối đi)
            finalSeats = allSeats.filter(seat => !seat.isBooked && seat.seatType !== 'aisle');
        } else {
            // Mặc định: trả về tất cả các ghế (không phải lối đi)
            finalSeats = allSeats.filter(seat => seat.seatType !== 'aisle');
        }

        return res.json(finalSeats);

    } catch (err) {
        console.error(`Lỗi lấy ghế cho chuyến ${tripId}:`, err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});


// GET /api/trips/:id/pickup-locations
router.get("/trips/:id/pickup-locations", async (req, res) => {
    const { id } = req.params;
    try {
        const result = await db.query(
            `SELECT rs.id, rs.name, rs.address, rs.type, rs.order_index, r.origin
             FROM route_stops rs
             JOIN trips t ON t.route_id = rs.route_id
             JOIN routes r ON r.id = t.route_id
             WHERE t.id = $1
               AND (rs.type = 'pickup' OR rs.type = 'both')
             ORDER BY rs.order_index`,
            [id]
        );

        if (!result.rows || result.rows.length === 0) {
            return res.json([]);
        }

        const origin = result.rows[0].origin || '';
        const filtered = result.rows.filter(stop => {
            const address = stop.address || stop.name || '';
            const provinceMap = {
                'đà lạt': ['lâm đồng', 'đà lạt'],
                'dalat': ['lâm đồng', 'đà lạt', 'lam dong'],
                'tp.hcm': ['tp.hcm', 'hồ chí minh', 'ho chi minh', 'sài gòn', 'saigon'],
                'hà nội': ['hà nội', 'ha noi', 'hanoi'],
                'đà nẵng': ['đà nẵng', 'da nang'],
                'nha trang': ['khánh hòa', 'nha trang', 'khanh hoa'],
                'huế': ['thừa thiên huế', 'huế', 'hue'],
                'cần thơ': ['cần thơ', 'can tho'],
                'vũng tàu': ['bà rịa vũng tàu', 'vũng tàu', 'vung tau']
            };

            const originLower = origin.toLowerCase().trim();
            let keywords = [];

            for (const [key, values] of Object.entries(provinceMap)) {
                if (originLower.includes(key) || key.includes(originLower)) {
                    keywords = values;
                    break;
                }
            }

            if (keywords.length === 0) {
                keywords = [originLower];
            }

            const addressLower = address.toLowerCase();
            const nameLower = (stop.name || '').toLowerCase();

            return keywords.some(keyword =>
                addressLower.includes(keyword) || nameLower.includes(keyword)
            );
        });

        console.log(`✅ [Pickup Locations] Trip ${id} (${origin}): Found ${filtered.length}/${result.rows.length} pickup points`);
        res.json(filtered);
    } catch (error) {
        console.error('❌ Error fetching pickup locations:', error);
        res.status(500).json({ message: "Lỗi server" });
    }
});

// GET /api/trips/:id/dropoff-locations
router.get("/trips/:id/dropoff-locations", async (req, res) => {
    const { id } = req.params;
    try {
        const result = await db.query(
            `SELECT rs.id, rs.name, rs.address, rs.type, rs.order_index, r.destination
             FROM route_stops rs
             JOIN trips t ON t.route_id = rs.route_id
             JOIN routes r ON r.id = t.route_id
             WHERE t.id = $1
               AND (rs.type = 'dropoff' OR rs.type = 'both')
             ORDER BY rs.order_index`,
            [id]
        );

        if (!result.rows || result.rows.length === 0) {
            return res.json([]);
        }
        const destination = result.rows[0].destination || '';
        const filtered = result.rows.filter(stop => {
            const address = stop.address || stop.name || '';
            const provinceMap = {
                'đà lạt': ['lâm đồng', 'đà lạt'],
                'dalat': ['lâm đồng', 'đà lạt', 'lam dong'],
                'tp.hcm': ['tp.hcm', 'hồ chí minh', 'ho chi minh', 'sài gòn', 'saigon'],
                'hà nội': ['hà nội', 'ha noi', 'hanoi'],
                'đà nẵng': ['đà nẵng', 'da nang'],
                'nha trang': ['khánh hòa', 'nha trang', 'khanh hoa'],
                'huế': ['thừa thiên huế', 'huế', 'hue'],
                'cần thơ': ['cần thơ', 'can tho'],
                'vũng tàu': ['bà rịa vũng tàu', 'vũng tàu', 'vung tau']
            };
            const destLower = destination.toLowerCase().trim();
            let keywords = [];

            for (const [key, values] of Object.entries(provinceMap)) {
                if (destLower.includes(key) || key.includes(destLower)) {
                    keywords = values;
                    break;
                }
            }

            if (keywords.length === 0) {
                keywords = [destLower];
            }

            const addressLower = address.toLowerCase();
            const nameLower = (stop.name || '').toLowerCase();

            return keywords.some(keyword =>
                addressLower.includes(keyword) || nameLower.includes(keyword)
            );
        });

        console.log(`✅ [Dropoff Locations] Trip ${id} (${destination}): Found ${filtered.length}/${result.rows.length} dropoff points`);
        res.json(filtered);
    } catch (error) {
        console.error('❌ Error fetching dropoff locations:', error);
        res.status(500).json({ message: "Lỗi server" });
    }
});

module.exports = router;

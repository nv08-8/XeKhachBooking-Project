// backend_api/routes/adminRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// ============================================================
// MIDDLEWARE: Kiểm tra quyền admin
// ============================================================
const checkAdminRole = async (req, res, next) => {
  const userId = req.headers["user-id"];
  if (!userId) {
    return res.status(401).json({ message: "Unauthorized: Missing user ID" });
  }

  try {
    const result = await db.query(
      "SELECT role FROM users WHERE id = $1",
      [userId]
    );
    if (!result.rows.length || result.rows[0].role !== "admin") {
      return res.status(403).json({ message: "Forbidden: Admin access required" });
    }
    next();
  } catch (err) {
    console.error("Error checking admin role:", err);
    return res.status(500).json({ message: "Server error" });
  }
};

// ============================================================// ROUTES: QUẢN LÝ TUYẾN XE// ============================================================

// Lấy một tuyến xe theo ID
router.get("/routes/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    try {
        const result = await db.query("SELECT * FROM routes WHERE id = $1", [id]);
        if (result.rows.length === 0) {
            return res.status(404).json({ message: "Tuyến xe không tìm thấy" });
        }
        res.json(result.rows[0]);
    } catch (err) {
        console.error(`Error fetching route with id ${id}:`, err);
        res.status(500).json({ message: "Lỗi khi lấy thông tin tuyến xe" });
    }
});

// 1. Thêm tuyến xe mới
router.post("/routes", checkAdminRole, async (req, res) => {
  const { origin, destination, distance_km, duration_min } = req.body;

  if (!origin || !destination || distance_km === undefined || duration_min === undefined) {
    return res.status(400).json({ message: "Thiếu thông tin bắt buộc" });
  }

  try {
    const result = await db.query(
      `INSERT INTO routes (origin, destination, distance_km, duration_min, created_at)
       VALUES ($1, $2, $3, $4, NOW())
       RETURNING *`,
      [origin, destination, distance_km, duration_min]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error("Error adding route:", err);
    res.status(500).json({ message: "Lỗi khi thêm tuyến xe" });
  }
});

// 2. Cập nhật tuyến xe
router.put("/routes/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const { origin, destination, distance_km, duration_min } = req.body;

  try {
    const result = await db.query(
      `UPDATE routes
       SET origin=$1, destination=$2, distance_km=$3, duration_min=$4
       WHERE id=$5
       RETURNING *`,
      [origin, destination, distance_km, duration_min, id]
    );
    if (!result.rows.length) {
      return res.status(404).json({ message: "Tuyến xe không tìm thấy" });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error("Error updating route:", err);
    res.status(500).json({ message: "Lỗi khi cập nhật tuyến xe" });
  }
});

// 3. Xóa tuyến xe
router.delete("/routes/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await db.query("DELETE FROM routes WHERE id=$1 RETURNING id", [id]);
    if (!result.rows.length) {
      return res.status(404).json({ message: "Tuyến xe không tìm thấy" });
    }
    res.json({ message: "Xóa tuyến xe thành công" });
  } catch (err) {
    console.error("Error deleting route:", err);
    res.status(500).json({ message: "Lỗi khi xóa tuyến xe" });
  }
});

// ============================================================// ROUTES: QUẢN LÝ CHUYẾN XE// ============================================================

// 1. Thêm chuyến xe mới
router.post("/trips", checkAdminRole, async (req, res) => {
  const {
    route_id,
    operator,
    bus_type,
    departure_time,
    arrival_time,
    price,
    seats_total,
  } = req.body;

  if (
    !route_id ||
    !operator ||
    !bus_type ||
    !departure_time ||
    !arrival_time ||
    !price ||
    !seats_total
  ) {
    return res.status(400).json({ message: "Thiếu thông tin bắt buộc" });
  }

  try {
    const result = await db.query(
      `INSERT INTO trips (route_id, operator, bus_type, departure_time, arrival_time, price, seats_total, seats_available, status, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'active', NOW())
       RETURNING *`,
      [route_id, operator, bus_type, departure_time, arrival_time, price, seats_total, seats_total]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error("Error adding trip:", err);
    res.status(500).json({ message: "Lỗi khi thêm chuyến xe" });
  }
});

// 2. Cập nhật chuyến xe
router.put("/trips/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const {
    operator,
    bus_type,
    departure_time,
    arrival_time,
    price,
    status,
  } = req.body;

  try {
    const result = await db.query(
      `UPDATE trips
       SET operator=$1, bus_type=$2, departure_time=$3, arrival_time=$4, price=$5, status=$6
       WHERE id=$7
       RETURNING *`,
      [operator, bus_type, departure_time, arrival_time, price, status, id]
    );
    if (!result.rows.length) {
      return res.status(404).json({ message: "Chuyến xe không tìm thấy" });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error("Error updating trip:", err);
    res.status(500).json({ message: "Lỗi khi cập nhật chuyến xe" });
  }
});

// 3. Xóa chuyến xe
router.delete("/trips/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await db.query("DELETE FROM trips WHERE id=$1 RETURNING id", [id]);
    if (!result.rows.length) {
      return res.status(404).json({ message: "Chuyến xe không tìm thấy" });
    }
    res.json({ message: "Xóa chuyến xe thành công" });
  } catch (err) {
    console.error("Error deleting trip:", err);
    res.status(500).json({ message: "Lỗi khi xóa chuyến xe" });
  }
});

// ============================================================// ROUTES: QUẢN LÝ ĐẶT VÉ// ============================================================

// 1. Lấy danh sách tất cả đặt vé (có thể lọc)
router.get("/bookings", checkAdminRole, async (req, res) => {
  const { trip_id, user_id, status, page = 1, page_size = 50 } = req.query;
  const offset = (page - 1) * page_size;

  let sql = `
    SELECT b.*, u.name, u.email, t.departure_time, r.origin, r.destination
    FROM bookings b
    JOIN users u ON u.id = b.user_id
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE 1=1
  `;
  const params = [];

  if (trip_id) {
    sql += " AND b.trip_id = $" + (params.length + 1);
    params.push(trip_id);
  }
  if (user_id) {
    sql += " AND b.user_id = $" + (params.length + 1);
    params.push(user_id);
  }
  if (status) {
    sql += " AND b.status = $" + (params.length + 1);
    params.push(status);
  }

  sql += " ORDER BY b.created_at DESC LIMIT $" + (params.length + 1) + " OFFSET $" + (params.length + 2);
  params.push(page_size, offset);

  try {
    const result = await db.query(sql, params);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching bookings:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách đặt vé" });
  }
});

// 2. Xác nhận đặt vé (chuyển từ pending → confirmed)
router.put("/bookings/:id/confirm", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await db.query(
      `UPDATE bookings SET status='confirmed' WHERE id=$1 RETURNING *`,
      [id]
    );
    if (!result.rows.length) {
      return res.status(404).json({ message: "Đặt vé không tìm thấy" });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error("Error confirming booking:", err);
    res.status(500).json({ message: "Lỗi khi xác nhận đặt vé" });
  }
});

// 3. Hủy đặt vé
router.put("/bookings/:id/cancel", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // Lấy thông tin booking
    const bookingResult = await client.query(
      "SELECT trip_id, seats_count FROM bookings WHERE id=$1",
      [id]
    );

    if (!bookingResult.rows.length) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Đặt vé không tìm thấy" });
    }

    const { trip_id, seats_count } = bookingResult.rows[0];

    // Cập nhật trạng thái booking
    await client.query(
      "UPDATE bookings SET status='cancelled' WHERE id=$1",
      [id]
    );

    // Tăng số ghế trống (nếu seats_count > 0)
    if (seats_count > 0) {
        await client.query(
          "UPDATE trips SET seats_available = seats_available + $1 WHERE id=$2",
          [seats_count, trip_id]
        );
    }

    await client.query("COMMIT");
    client.release();

    res.json({ message: "Hủy đặt vé thành công" });
  } catch (err) {
    await client.query("ROLLBACK");
    client.release();
    console.error("Error cancelling booking:", err);
    res.status(500).json({ message: "Lỗi khi hủy đặt vé" });
  }
});

// ============================================================// ROUTES: QUẢN LÝ NGƯỜI DÙNG// ============================================================

router.get("/users", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query("SELECT id, name, email, phone, role, status FROM users ORDER BY id ASC");
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching users:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách người dùng" });
  }
});

router.put("/users/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const { name, email, phone, role, status } = req.body;

  try {
    const result = await db.query(
      `UPDATE users
       SET name=$1, email=$2, phone=$3, role=$4, status=$5
       WHERE id=$6
       RETURNING *`,
      [name, email, phone, role, status, id]
    );
    if (!result.rows.length) {
      return res.status(404).json({ message: "Người dùng không tìm thấy" });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error("Error updating user:", err);
    res.status(500).json({ message: "Lỗi khi cập nhật người dùng" });
  }
});

router.delete("/users/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await db.query("DELETE FROM users WHERE id=$1 RETURNING id", [id]);
    if (!result.rows.length) {
      return res.status(404).json({ message: "Người dùng không tìm thấy" });
    }
    res.json({ message: "Xóa người dùng thành công" });
  } catch (err) {
    console.error("Error deleting user:", err);
    res.status(500).json({ message: "Lỗi khi xóa người dùng" });
  }
});

// ============================================================// ROUTES: BÁO CÁO DOANH THU// ============================================================

// 1. Doanh thu theo tuyến xe
router.get("/revenue/by-route", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        r.id,
        r.origin,
        r.destination,
        COUNT(b.id) as total_bookings,
        SUM(b.total_amount) as total_revenue
      FROM routes r
      JOIN trips t ON t.route_id = r.id
      JOIN bookings b ON b.trip_id = t.id AND b.status = 'confirmed'
      GROUP BY r.id, r.origin, r.destination
      ORDER BY total_revenue DESC NULLS LAST
    `);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue by route:", err);
    res.status(500).json({ message: "Lỗi khi lấy doanh thu theo tuyến" });
  }
});

// 2. Doanh thu theo ngày
router.get("/revenue/by-date", checkAdminRole, async (req, res) => {
  const { from_date, to_date } = req.query;

  let sql = `
    SELECT
      DATE(b.created_at) as date,
      COUNT(b.id) as total_bookings,
      SUM(b.total_amount) as total_revenue
    FROM bookings b
    WHERE b.status = 'confirmed'
  `;
  const params = [];

  if (from_date) {
    sql += " AND b.created_at >= $" + (params.length + 1);
    params.push(from_date);
  }
  if (to_date) {
    sql += " AND b.created_at <= $" + (params.length + 1);
    params.push(to_date);
  }

  sql += " GROUP BY DATE(b.created_at) ORDER BY date DESC";

  try {
    const result = await db.query(sql, params);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue by date:", err);
    res.status(500).json({ message: "Lỗi khi lấy doanh thu theo ngày" });
  }
});

// 3. Doanh thu theo tháng
router.get("/revenue/by-month", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        TO_CHAR(b.created_at, 'YYYY-MM') as month,
        COUNT(b.id) as total_bookings,
        SUM(b.total_amount) as total_revenue
      FROM bookings b
      WHERE b.status = 'confirmed'
      GROUP BY TO_CHAR(b.created_at, 'YYYY-MM')
      ORDER BY month DESC
    `);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue by month:", err);
    res.status(500).json({ message: "Lỗi khi lấy doanh thu theo tháng" });
  }
});

// 4. Doanh thu theo năm
router.get("/revenue/by-year", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        EXTRACT(YEAR FROM b.created_at) as year,
        COUNT(b.id) as total_bookings,
        SUM(b.total_amount) as total_revenue
      FROM bookings b
      WHERE b.status = 'confirmed'
      GROUP BY EXTRACT(YEAR FROM b.created_at)
      ORDER BY year DESC
    `);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue by year:", err);
    res.status(500).json({ message: "Lỗi khi lấy doanh thu theo năm" });
  }
});

// 5. Chi tiết doanh thu
router.get("/revenue/details", checkAdminRole, async (req, res) => {
  const { group_by, value } = req.query;
  let sql = `
    SELECT
      b.id AS booking_id,
      u.name AS user_name,
      r.origin || ' - ' || r.destination AS route_info,
      t.departure_time,
      b.seats_count AS ticket_count,
      b.total_amount AS total_price
    FROM bookings b
    JOIN users u ON u.id = b.user_id
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.status = 'confirmed'
  `;
  const params = [];

  if (group_by === "day") {
    sql += ` AND DATE(b.created_at) = $1`;
    params.push(value);
  } else if (group_by === "month") {
    sql += ` AND TO_CHAR(b.created_at, 'YYYY-MM') = $1`;
    params.push(value);
  } else if (group_by === "year") {
    sql += ` AND EXTRACT(YEAR FROM b.created_at) = $1`;
    params.push(value);
  } else if (group_by === "route") {
    // Assuming value is route_id
    sql += ` AND t.route_id = $1`;
    params.push(value);
  }

  sql += " ORDER BY t.departure_time DESC";

  try {
    const result = await db.query(sql, params);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue details:", err);
    res.status(500).json({ message: "Lỗi khi lấy chi tiết doanh thu" });
  }
});


// ======================// ROUTES: QUẢN LÝ KHUYẾN MÃI// ============================================================

// GET all promotions
router.get("/promotions", checkAdminRole, async (req, res) => {
    try {
        const { rows } = await db.query("SELECT * FROM promotions ORDER BY id ASC");
        res.json(rows);
    } catch (err) {
        console.error("Error fetching promotions:", err);
        res.status(500).json({ message: "Lỗi khi lấy danh sách khuyến mãi" });
    }
});

// GET promotion by ID
router.get("/promotions/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    try {
        const { rows } = await db.query("SELECT * FROM promotions WHERE id = $1", [id]);
        if (rows.length === 0) {
            return res.status(404).json({ message: "Không tìm thấy khuyến mãi" });
        }
        res.json(rows[0]);
    } catch (err) {
        console.error(`Error fetching promotion ${id}:`, err);
        res.status(500).json({ message: "Lỗi khi lấy thông tin khuyến mãi" });
    }
});

// POST a new promotion
router.post("/promotions", checkAdminRole, async (req, res) => {
    const { code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status } = req.body;
    try {
        const { rows } = await db.query(
            `INSERT INTO promotions (code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
            [code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status]
        );
        res.status(201).json(rows[0]);
    } catch (err) {
        console.error("Error creating promotion:", err);
        res.status(500).json({ message: "Lỗi khi thêm khuyến mãi" });
    }
});

// PUT (update) a promotion
router.put("/promotions/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    const { code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status } = req.body;
    try {
        const { rows } = await db.query(
            `UPDATE promotions SET code=$1, discount_type=$2, discount_value=$3, min_price=$4, max_discount=$5, start_date=$6, end_date=$7, status=$8 
             WHERE id=$9 RETURNING *`,
            [code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status, id]
        );
        if (rows.length === 0) {
            return res.status(404).json({ message: "Không tìm thấy khuyến mãi" });
        }
        res.json(rows[0]);
    } catch (err) {
        console.error(`Error updating promotion ${id}:`, err);
        res.status(500).json({ message: "Lỗi khi cập nhật khuyến mãi" });
    }
});

// DELETE a promotion
router.delete("/promotions/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    try {
        const result = await db.query("DELETE FROM promotions WHERE id = $1 RETURNING *", [id]);
        if (result.rowCount === 0) {
            return res.status(404).json({ message: "Không tìm thấy khuyến mãi" });
        }
        res.status(200).json({ message: "Xóa khuyến mãi thành công" });
    } catch (err) {
        console.error("Error deleting promotion:", err);
        res.status(500).json({ message: "Lỗi khi xóa khuyến mãi" });
    }
});

// ============================================================
// ROUTES: QUẢN LÝ XÁC NHẬN THANH TOÁN OFFLINE
// ============================================================

// GET /admin/pending-offline-payments - Get list of pending offline payments
router.get("/pending-offline-payments", checkAdminRole, async (req, res) => {
    try {
        const sql = `
            SELECT
                b.id,
                b.user_id,
                b.trip_id,
                b.status,
                b.total_amount,
                b.payment_method,
                b.created_at,
                u.name AS user_name,
                u.phone AS user_phone,
                t.departure_time,
                t.arrival_time,
                t.operator,
                r.origin,
                r.destination,
                COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) AS seat_labels
            FROM bookings b
            JOIN users u ON u.id = b.user_id
            JOIN trips t ON t.id = b.trip_id
            JOIN routes r ON r.id = t.route_id
            LEFT JOIN booking_items bi ON bi.booking_id = b.id
            WHERE b.status = 'pending'
            AND b.payment_method IN ('cash', 'offline', 'cod', 'counter')
            GROUP BY b.id, u.id, t.id, r.id
            ORDER BY b.created_at DESC
        `;
        const result = await db.query(sql);
        res.json(result.rows);
    } catch (err) {
        console.error("Error fetching pending offline payments:", err);
        res.status(500).json({ message: "Lỗi khi lấy danh sách thanh toán chờ xác nhận" });
    }
});

// POST /admin/confirm-offline-payment/:id - Admin confirms offline payment
router.post("/confirm-offline-payment/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    const client = await db.connect();
    try {
        await client.query('BEGIN');

        const bookingRes = await client.query(
            `SELECT b.id, b.trip_id, b.status, b.total_amount, b.payment_method, b.user_id
             FROM bookings b WHERE b.id=$1 FOR UPDATE`,
            [id]
        );

        if (!bookingRes.rowCount) {
            await client.query('ROLLBACK');
            client.release();
            return res.status(404).json({ message: 'Booking not found' });
        }

        const booking = bookingRes.rows[0];

        // Verify this is an offline payment
        const normalizedMethod = (booking.payment_method || '').toLowerCase();
        const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);

        if (!isOfflinePayment) {
            await client.query('ROLLBACK');
            client.release();
            return res.status(400).json({
                message: 'Can only confirm offline payments',
                payment_method: booking.payment_method
            });
        }

        if (booking.status !== 'pending') {
            await client.query('ROLLBACK');
            client.release();
            return res.status(409).json({
                message: 'Booking must be in pending status',
                current_status: booking.status
            });
        }

        // Update booking status to confirmed
        const totalAmount = Number(booking.total_amount) || 0;
        const userId = booking.user_id;
        const adminUserId = req.headers["user-id"];

        const paymentMeta = {
            method: normalizedMethod,
            confirmed_by: 'admin',
            admin_id: adminUserId,
            confirmed_at: new Date().toISOString(),
            note: 'Offline payment confirmed by admin'
        };

        await client.query(
            `UPDATE bookings
             SET status=$1, price_paid=$2, paid_at=NOW(), metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
             WHERE id=$4`,
            ['confirmed', totalAmount, JSON.stringify({ payment: paymentMeta }), id]
        );

        await client.query('COMMIT');
        client.release();

        res.json({
            message: 'Xác nhận thanh toán thành công',
            booking_id: Number(id),
            status: 'confirmed',
            price_paid: totalAmount
        });

    } catch (err) {
        console.error('Confirm offline payment failed:', err.message || err);
        try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
        client.release();
        res.status(500).json({ message: 'Lỗi khi xác nhận thanh toán' });
    }
});

module.exports = router;

// backend_api/routes/adminRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateBookingCode } = require("../utils/bookingHelper");
const { generateDetailedSeatLayout } = require("../data/seat_layout");

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
    LEFT JOIN users u ON u.id = b.user_id
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
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // Lấy booking để xử lý trong transaction
    const bookingRes = await client.query(
      "SELECT id, total_amount, price_paid, payment_method, status FROM bookings WHERE id=$1 FOR UPDATE",
      [id]
    );

    if (!bookingRes.rowCount) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Đặt vé không tìm thấy" });
    }

    const booking = bookingRes.rows[0];

    console.log(`[admin.confirm] booking id=${id} loaded`, { booking });

    // Only allow confirming pending or expired bookings (admin override)
    if (!['pending', 'expired', 'pending_refund'].includes(String(booking.status))) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(409).json({ message: 'Không thể xác nhận đặt vé ở trạng thái hiện tại', status: booking.status });
    }

    // Safely parse amounts (handle strings like "0.00")
    let paidAmount = parseFloat(booking.price_paid);
    if (Number.isNaN(paidAmount) || paidAmount <= 0) {
      paidAmount = parseFloat(booking.total_amount) || 0;
    }

    // Ensure paidAmount is a number (DB expects numeric)
    if (Number.isNaN(paidAmount)) paidAmount = 0;

    const updateRes = await client.query(
      `UPDATE bookings SET status='confirmed', price_paid=$1 WHERE id=$2 RETURNING *`,
      [paidAmount, id]
    );

    if (!updateRes || !updateRes.rows || updateRes.rows.length === 0) {
      // This is unexpected — rollback and return error
      await client.query("ROLLBACK");
      client.release();
      console.error(`[admin.confirm] failed to update booking id=${id}`);
      return res.status(500).json({ message: 'Không thể cập nhật đặt vé sau khi xác nhận' });
    }

    await client.query("COMMIT");
    client.release();

    console.log(`[admin.confirm] booking id=${id} confirmed, paidAmount=${paidAmount}`);
    res.json(updateRes.rows[0]);
  } catch (err) {
    try { await client.query("ROLLBACK"); } catch (e) { console.error('rollback failed', e); }
    try { client.release(); } catch (e) { /* ignore */ }
    console.error("Error confirming booking by admin:", err && err.stack ? err.stack : err);
    // Return error message to help debugging in development (omit in production)
    res.status(500).json({ message: "Lỗi khi xác nhận đặt vé", error: err && err.message ? err.message : String(err) });
  }
});

// 3. Hủy đặt vé (admin)
router.put("/bookings/:id/cancel", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // Lấy thông tin booking (kèm trạng thái) và khoá để tránh race
    const bookingResult = await client.query(
      "SELECT id, trip_id, seats_count, status FROM bookings WHERE id=$1 FOR UPDATE",
      [id]
    );

    if (!bookingResult.rows.length) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Đặt vé không tìm thấy" });
    }

    const { trip_id, seats_count, status } = bookingResult.rows[0];

    // Admin can cancel most statuses (confirmed, pending, pending_refund, expired)
    if (!['confirmed', 'pending', 'pending_refund', 'expired'].includes(status)) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(409).json({ message: "Không thể hủy đặt vé ở trạng thái hiện tại", status });
    }

    // Update booking status and cancelled_at
    await client.query(
      "UPDATE bookings SET status='cancelled', cancelled_at=NOW() WHERE id=$1",
      [id]
    );

    // Release seat records tied to this booking (if any)
    const releaseRes = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1 RETURNING id', [id]);
    const releasedCount = releaseRes.rowCount || 0;

    // If seats_count exists and seats table didn't contain rows (rare), fall back to seats_count
    const toAdd = releasedCount > 0 ? releasedCount : (seats_count || 0);
    if (toAdd > 0) {
      await client.query(
        "UPDATE trips SET seats_available = seats_available + $1 WHERE id=$2",
        [toAdd, trip_id]
      );
    }

    await client.query("COMMIT");
    client.release();

    res.json({ message: "Hủy đặt vé thành công" });
  } catch (err) {
    try { await client.query("ROLLBACK"); } catch (e) {}
    client.release();
    console.error("Error cancelling booking (admin):", err);
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

router.get("/revenue", checkAdminRole, async (req, res) => {
    const { groupBy, route_id, trip_id, from_date, to_date } = req.query;

    let query = `
        SELECT
            %s AS group_key,
            COUNT(b.id) AS total_bookings,
            SUM(b.total_amount) AS total_revenue
        FROM bookings b
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        WHERE b.status = 'confirmed'
    `;
    const params = [];

    let groupByClause;
    let orderByClause;

    switch (groupBy) {
        case 'day':
        case 'date':
            groupByClause = "DATE(b.created_at)";
            orderByClause = "group_key DESC";
            if (from_date) {
                params.push(from_date);
                query += ` AND b.created_at >= $${params.length}`;
            }
            if (to_date) {
                params.push(to_date);
                query += ` AND b.created_at <= $${params.length}`;
            }
            break;
        case 'month':
            groupByClause = "TO_CHAR(b.created_at, 'YYYY-MM')";
            orderByClause = "group_key DESC";
            break;
        case 'year':
            groupByClause = "EXTRACT(YEAR FROM b.created_at)";
            orderByClause = "group_key DESC";
            break;
        case 'route':
            groupByClause = "r.id, r.origin, r.destination";
            orderByClause = "total_revenue DESC NULLS LAST";
            query = query.replace("%s", "r.id AS group_key, r.origin, r.destination");
            break;
        case 'trip':
            groupByClause = "t.id, t.departure_time, r.origin, r.destination";
            orderByClause = "total_revenue DESC NULLS LAST";
            query = query.replace("%s", "t.id AS group_key, t.departure_time, r.origin, r.destination");
            if (route_id) {
                params.push(route_id);
                query += ` AND t.route_id = $${params.length}`;
            }
            break;
        default:
            return res.status(400).json({ message: "Invalid groupBy value: " + groupBy });
    }

    if (groupBy !== 'route' && groupBy !== 'trip') {
        query = query.replace("%s", groupByClause);
    }

    if (trip_id) {
        params.push(trip_id);
        query += ` AND b.trip_id = $${params.length}`;
    }

    query += ` GROUP BY ${groupByClause} ORDER BY ${orderByClause}`;

    try {
        const result = await db.query(query, params);
        res.json(result.rows);
    } catch (err) {
        console.error(`Error fetching revenue by ${groupBy}:`, err);
        res.status(500).json({ message: `Lỗi khi lấy doanh thu theo ${groupBy}` });
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

  if (group_by === "day" || group_by === "date") {
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
  } else if (group_by === "trip") {
    sql += ` AND t.id = $1`;
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

        // Try to update with paid_at first, fallback if column doesn't exist
        try {
            await client.query(
                `UPDATE bookings
                 SET status=$1, price_paid=$2, paid_at=NOW(), metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
                 WHERE id=$4`,
                ['confirmed', totalAmount, JSON.stringify({ payment: paymentMeta }), id]
            );
        } catch (updateErr) {
            // If paid_at column doesn't exist, try without it
            if (updateErr.message && updateErr.message.includes('paid_at')) {
                await client.query(
                    `UPDATE bookings
                     SET status=$1, price_paid=$2, metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
                     WHERE id=$4`,
                    ['confirmed', totalAmount, JSON.stringify({ payment: paymentMeta }), id]
                );
            } else {
                throw updateErr;
            }
        }

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

// ============================================================
// ADMIN: CREATE BOOKING FOR OFFLINE/WALK-IN CUSTOMERS
// ============================================================
router.post("/bookings", checkAdminRole, async (req, res) => {
  const { trip_id, seat_labels } = req.body;

  if (!trip_id || !Array.isArray(seat_labels) || seat_labels.length === 0) {
    return res.status(400).json({ message: "Missing required fields: trip_id and seat_labels" });
  }

  const client = await db.connect();
  try {
    await client.query('BEGIN');

    // Lock trip record first (without JOIN)
    const tripLockResult = await client.query(
      `SELECT id FROM trips WHERE id = $1 FOR UPDATE`,
      [trip_id]
    );

    if (!tripLockResult.rowCount) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Trip not found" });
    }

    // Get trip details with bus info
    const tripResult = await client.query(
      `SELECT t.id, t.seats_available, t.bus_type, b.seat_layout
       FROM trips t
       LEFT JOIN buses b ON t.bus_type = b.bus_type
       WHERE t.id = $1`,
      [trip_id]
    );

    if (!tripResult.rowCount) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Trip not found" });
    }

    const trip = tripResult.rows[0];

    // Validate seats tồn tại trong seat_layout từ buses
    if (trip.seat_layout) {
      try {
        const layout = (typeof trip.seat_layout === 'string') ? JSON.parse(trip.seat_layout) : trip.seat_layout;
        const detailedLayout = generateDetailedSeatLayout(trip.bus_type, layout);

        // Lấy danh sách ghế hợp lệ từ layout
        const validSeats = new Set();
        if (detailedLayout.floors) {
          detailedLayout.floors.forEach(floor => {
            if (floor.seats) {
              floor.seats.forEach(s => validSeats.add(s.label));
            }
          });
        }

        // Kiểm tra seat_labels có nằm trong layout không
        for (const label of seat_labels) {
          if (!validSeats.has(label)) {
            await client.query("ROLLBACK");
            client.release();
            return res.status(400).json({
              message: `Ghế ${label} không tồn tại trong cấu trúc xe`
            });
          }
        }
      } catch (e) {
        console.error("Lỗi validate seat layout:", e);
      }
    }

    // Chỉ đánh dấu ghế là không còn trống (is_booked = 1)
    // Không tạo booking record vì khách mua trực tiếp tại nhà xe
    for (const label of seat_labels) {
      // Use UPSERT to mark seat as booked (no booking_id)
      await client.query(
        `INSERT INTO seats (trip_id, label, type, is_booked)
         VALUES ($1, $2, $3, 1)
         ON CONFLICT (trip_id, label) DO UPDATE
         SET is_booked=1`,
        [trip_id, label, 'seat']
      );
    }

    // Cập nhật seats_available của trip
    await client.query(
      'UPDATE trips SET seats_available = seats_available - $1 WHERE id=$2',
      [seat_labels.length, trip_id]
    );

    await client.query('COMMIT');
    client.release();

    console.log(`✅ Admin marked ${seat_labels.length} seats as occupied for trip ${trip_id} (in-store purchase)`);

    res.status(201).json({
      message: `Đánh dấu ${seat_labels.length} ghế thành công (mua tại nhà xe)`,
      trip_id,
      seats_marked: seat_labels,
      seats_count: seat_labels.length
    });
  } catch (err) {
    try { await client.query("ROLLBACK"); } catch (e) { }
    client.release();
    console.error("Error marking seats as occupied:", err);
    res.status(500).json({ message: "Error marking seats", error: err.message });
  }
});

/* ============================================================
    TRIP MANAGEMENT (FULL INFO INCLUDING SEAT LAYOUT)
   ============================================================ */

router.get("/trips/:id", checkAdminRole, async (req, res) => {
    const { id } = req.params;
    try {
        const query = `
            SELECT 
                t.*, 
                r.origin, r.destination, r.distance_km, r.duration_min,
                COALESCE(b.seat_layout, (SELECT seat_layout FROM buses WHERE bus_type = t.bus_type LIMIT 1)) as seat_layout
            FROM trips t
            JOIN routes r ON t.route_id = r.id
            LEFT JOIN buses b ON t.bus_id = b.id
            WHERE t.id = $1
        `;
        const result = await db.query(query, [id]);
        if (result.rows.length === 0) {
            return res.status(404).json({ message: "Không tìm thấy chuyến đi" });
        }

        const trip = result.rows[0];
        
        // 1. Lấy trạng thái ghế đã đặt từ bảng seats
        const bookedSeatsRes = await db.query(
            "SELECT label FROM seats WHERE trip_id = $1 AND is_booked = 1",
            [id]
        );
        const bookedLabels = new Set(bookedSeatsRes.rows.map(r => r.label));

        // 2. Xử lý seat layout: Generate detailed seats từ layout cơ bản và merge isBooked status
        const { generateDetailedSeatLayout } = require('../data/seat_layout');
        if (trip.seat_layout) {
            try {
                let layout = (typeof trip.seat_layout === 'string') ? JSON.parse(trip.seat_layout) : trip.seat_layout;

                // Generate detailed seats nếu layout chưa có seats array
                const hasSeatsDetail = layout?.floors?.some(f =>
                    Array.isArray(f.seats) && f.seats.length > 0
                );

                if (!hasSeatsDetail) {
                    layout = generateDetailedSeatLayout(trip.bus_type, layout);
                }

                // Gán isBooked status từ bảng seats
                if (layout && layout.floors) {
                    layout.floors.forEach(floor => {
                        if (floor.seats) {
                            floor.seats = floor.seats.map(s => ({
                                ...s,
                                isBooked: bookedLabels.has(s.label)
                            }));
                        }
                    });
                }
                trip.seat_layout = layout;
            } catch (e) {
                console.error("Lỗi parse/generate seat_layout cho admin:", e);
            }
        }

        res.json(trip);
    } catch (err) {
        console.error("Error fetching trip for admin:", err);
        res.status(500).json({ message: "Lỗi server khi lấy thông tin chuyến đi" });
    }
});

/* ============================================================
    MIGRATION: Mở rộng seat_layout cho tất cả buses
   ============================================================ */
router.post("/migration/expand-seat-layouts", checkAdminRole, async (req, res) => {
    try {
        console.log('🔄 Bắt đầu mở rộng seat_layout cho tất cả buses...');

        const result = await db.query('SELECT id, bus_type, seat_layout FROM buses ORDER BY id');
        const buses = result.rows;

        let updated = 0;
        let skipped = 0;
        const errors = [];

        for (const bus of buses) {
            try {
                let layout = bus.seat_layout;

                // Parse nếu là string
                if (typeof layout === 'string') {
                    layout = JSON.parse(layout);
                }

                // Kiểm tra nếu layout đã có seats detail
                const hasSeatsDetail = layout?.floors?.some(f =>
                    Array.isArray(f.seats) && f.seats.length > 0
                );

                if (hasSeatsDetail) {
                    skipped++;
                    continue;
                }

                // Expand layout
                const expandedLayout = generateDetailedSeatLayout(bus.bus_type, layout);
                const expandedLayoutJson = JSON.stringify(expandedLayout);

                // Update database
                await db.query(
                    'UPDATE buses SET seat_layout = $1 WHERE id = $2',
                    [expandedLayoutJson, bus.id]
                );

                updated++;
                console.log(`✅ Bus ${bus.id} (${bus.bus_type}) - expanded`);

            } catch (e) {
                errors.push({ bus_id: bus.id, bus_type: bus.bus_type, error: e.message });
                console.error(`❌ Error updating bus ${bus.id}:`, e.message);
            }
        }

        res.json({
            message: `Migration hoàn tất!`,
            summary: {
                total: buses.length,
                updated,
                skipped,
                errors: errors.length > 0 ? errors : []
            }
        });

    } catch (err) {
        console.error('❌ Migration failed:', err);
        res.status(500).json({ message: 'Migration failed', error: err.message });
    }
});

module.exports = router;

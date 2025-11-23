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

// ============================================================
// ROUTES: QUẢN LÝ TUYẾN XE
// ============================================================

// 1. Thêm tuyến xe mới
router.post("/admin/routes", checkAdminRole, async (req, res) => {
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
router.put("/admin/routes/:id", checkAdminRole, async (req, res) => {
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
router.delete("/admin/routes/:id", checkAdminRole, async (req, res) => {
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

// ============================================================
// ROUTES: QUẢN LÝ CHUYẾN XE
// ============================================================

// 1. Thêm chuyến xe mới
router.post("/admin/trips", checkAdminRole, async (req, res) => {
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
router.put("/admin/trips/:id", checkAdminRole, async (req, res) => {
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
router.delete("/admin/trips/:id", checkAdminRole, async (req, res) => {
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

// ============================================================
// ROUTES: QUẢN LÝ ĐẶT VÉ
// ============================================================

// 1. Lấy danh sách tất cả đặt vé (có thể lọc)
router.get("/admin/bookings", checkAdminRole, async (req, res) => {
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
router.put("/admin/bookings/:id/confirm", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    const result = await db.query(
      `UPDATE bookings SET status='confirmed', updated_at=NOW() WHERE id=$1 RETURNING *`,
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
router.put("/admin/bookings/:id/cancel", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // Lấy thông tin booking
    const bookingResult = await client.query(
      "SELECT seat_label, trip_id FROM bookings WHERE id=$1",
      [id]
    );

    if (!bookingResult.rows.length) {
      await client.query("ROLLBACK");
      client.release();
      return res.status(404).json({ message: "Đặt vé không tìm thấy" });
    }

    const { seat_label, trip_id } = bookingResult.rows[0];

    // Cập nhật trạng thái booking
    await client.query(
      "UPDATE bookings SET status='cancelled', updated_at=NOW() WHERE id=$1",
      [id]
    );

    // Giải phóng ghế
    await client.query(
      "UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=$1 AND label=$2",
      [trip_id, seat_label]
    );

    // Tăng số ghế trống
    await client.query(
      "UPDATE trips SET seats_available = seats_available + 1 WHERE id=$1",
      [trip_id]
    );

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

// ============================================================
// ROUTES: QUẢN LÝ NGƯỜI DÙNG
// ============================================================

router.get("/admin/users", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query("SELECT id, name, email, phone, role, status FROM users ORDER BY id ASC");
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching users:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách người dùng" });
  }
});

// ============================================================
// ROUTES: BÁO CÁO DOANH THU
// ============================================================

// 1. Doanh thu theo tuyến xe
router.get("/admin/revenue/by-route", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        r.id,
        r.origin,
        r.destination,
        COUNT(b.id) as total_bookings,
        SUM(CASE WHEN b.status='confirmed' THEN b.price_paid ELSE 0 END) as total_revenue,
        SUM(CASE WHEN b.status='confirmed' THEN 1 ELSE 0 END) as confirmed_bookings
      FROM routes r
      LEFT JOIN trips t ON t.route_id = r.id
      LEFT JOIN bookings b ON b.trip_id = t.id
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
router.get("/admin/revenue/by-date", checkAdminRole, async (req, res) => {
  const { from_date, to_date } = req.query;

  let sql = `
    SELECT
      DATE(b.created_at) as date,
      COUNT(b.id) as total_bookings,
      SUM(CASE WHEN b.status='confirmed' THEN b.price_paid ELSE 0 END) as total_revenue,
      SUM(CASE WHEN b.status='confirmed' THEN 1 ELSE 0 END) as confirmed_bookings
    FROM bookings b
    WHERE 1=1
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
router.get("/admin/revenue/by-month", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        TO_CHAR(b.created_at, 'YYYY-MM') as month,
        COUNT(b.id) as total_bookings,
        SUM(CASE WHEN b.status='confirmed' THEN b.price_paid ELSE 0 END) as total_revenue,
        SUM(CASE WHEN b.status='confirmed' THEN 1 ELSE 0 END) as confirmed_bookings
      FROM bookings b
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
router.get("/admin/revenue/by-year", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(`
      SELECT
        EXTRACT(YEAR FROM b.created_at) as year,
        COUNT(b.id) as total_bookings,
        SUM(CASE WHEN b.status='confirmed' THEN b.price_paid ELSE 0 END) as total_revenue,
        SUM(CASE WHEN b.status='confirmed' THEN 1 ELSE 0 END) as confirmed_bookings
      FROM bookings b
      GROUP BY EXTRACT(YEAR FROM b.created_at)
      ORDER BY year DESC
    `);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue by year:", err);
    res.status(500).json({ message: "Lỗi khi lấy doanh thu theo năm" });
  }
});

module.exports = router;


// backend_api/routes/adminRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateBookingCode } = require("../utils/bookingHelper");
const { generateDetailedSeatLayout } = require("../data/seat_layout");
const sendPaymentConfirmationEmail = require("../utils/sendPaymentEmail");

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
    console.log("[admin.routes.POST] Attempting to insert/update route:", { origin, destination, distance_km, duration_min });

    // Step 1: Check if route exists by (origin, destination) using EXACT match
    const checkResult = await db.query(
      `SELECT id FROM routes WHERE origin = $1 AND destination = $2 LIMIT 1`,
      [origin, destination]
    );

    if (checkResult.rowCount > 0) {
      // Route exists - UPDATE it
      const routeId = checkResult.rows[0].id;
      console.log(`[admin.routes.POST] Route exists (id=${routeId}), updating...`);
      const result = await db.query(
        `UPDATE routes SET distance_km = $1, duration_min = $2 WHERE id = $3 RETURNING *`,
        [distance_km, duration_min, routeId]
      );
      console.log("[admin.routes.POST] Route updated:", result.rows[0]);
      res.status(200).json(result.rows[0]);
    } else {
      // Route doesn't exist - INSERT new one
      console.log("[admin.routes.POST] Route doesn't exist, inserting new...");

      // Get next available ID (since sequence might not exist)
      const maxIdRes = await db.query(`SELECT COALESCE(MAX(id), 0)::INTEGER as max_id FROM routes`);
      const maxId = parseInt(maxIdRes.rows[0].max_id, 10); // Ensure it's a number
      const nextId = maxId + 1;
      console.log(`[admin.routes.POST] Max ID: ${maxId}, Next available ID: ${nextId}`);

      const result = await db.query(
        `INSERT INTO routes (id, origin, destination, distance_km, duration_min, created_at)
         VALUES ($1, $2, $3, $4, $5, NOW())
         RETURNING *`,
        [nextId, origin, destination, distance_km, duration_min]
      );
      console.log("[admin.routes.POST] Route inserted:", result.rows[0]);
      res.status(201).json(result.rows[0]);
    }
  } catch (err) {
    console.error("[admin.routes.POST] Error:", err.message || err);
    console.error("[admin.routes.POST] Error code:", err.code);
    console.error("[admin.routes.POST] Constraint:", err.constraint);

    // If it's a unique constraint violation, route exists
    if (err.code === '23505' || err.code === '23514') {
      try {
        console.log("[admin.routes.POST] Unique constraint violation - route already exists, trying UPDATE...");
        const checkResult = await db.query(
          `SELECT id FROM routes WHERE origin = $1 AND destination = $2 LIMIT 1`,
          [origin, destination]
        );
        if (checkResult.rowCount > 0) {
          const routeId = checkResult.rows[0].id;
          const result = await db.query(
            `UPDATE routes SET distance_km = $1, duration_min = $2 WHERE id = $3 RETURNING *`,
            [distance_km, duration_min, routeId]
          );
          console.log("[admin.routes.POST] Fallback UPDATE successful:", result.rows[0]);
          res.status(200).json(result.rows[0]);
        } else {
          res.status(500).json({ message: "Lỗi khi thêm tuyến xe", error: err.message });
        }
      } catch (fallbackErr) {
        console.error("[admin.routes.POST] Fallback failed:", fallbackErr.message);
        res.status(500).json({ message: "Lỗi khi thêm tuyến xe", error: fallbackErr.message });
      }
    } else {
      res.status(500).json({ message: "Lỗi khi thêm tuyến xe", error: err.message });
    }
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
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'scheduled', NOW())
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

// 3. Xóa/Hủy chuyến xe
router.delete("/trips/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;

  try {
    // Cập nhật status thành 'cancelled' thay vì xóa (để giữ lịch sử)
    const result = await db.query(
      "UPDATE trips SET status=$1 WHERE id=$2 RETURNING id",
      ['cancelled', id]
    );

    if (!result.rows.length) {
      return res.status(404).json({ message: "Chuyến xe không tìm thấy" });
    }

    res.json({
      message: "Chuyến xe đã được hủy. Khách hàng sẽ nhận được thông báo hoàn tiền.",
      trip_id: result.rows[0].id
    });
  } catch (err) {
    console.error("Error cancelling trip:", err);
    res.status(500).json({ message: "Lỗi khi hủy chuyến xe" });
  }
});

// ============================================================// ROUTES: QUẢN LÝ ĐẶT VÉ// ============================================================

// 1. Lấy danh sách tất cả đặt vé (có thể lọc)
router.get("/bookings", checkAdminRole, async (req, res) => {
  const { trip_id, user_id, status, page = 1, page_size = 50 } = req.query;
  const offset = (page - 1) * page_size;

  let sql = `
    SELECT b.*, u.name, u.email, t.departure_time, t.status AS trip_status, r.origin, r.destination,
           CASE
             WHEN b.status = 'confirmed' AND t.status = 'cancelled' THEN 'Vé đã thanh toán nhưng chuyến bị hủy'
             WHEN b.status = 'cancelled' THEN 'Vé đã bị hủy'
             ELSE NULL
           END AS cancellation_message
    FROM bookings b
    LEFT JOIN users u ON u.id = b.user_id
    LEFT JOIN trips t ON t.id = b.trip_id
    LEFT JOIN routes r ON r.id = t.route_id
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
      `UPDATE bookings SET status='confirmed', price_paid=$1, paid_at=NOW() WHERE id=$2 RETURNING *`,
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

    // Send confirmation email
    try {
      console.log(`[admin.confirm] Attempting to send email for booking ${id}`);
      const fullBooking = await db.query(
        `SELECT b.*, u.email, u.name, u.phone, r.origin, r.destination, t.departure_time, t.operator, t.bus_type,
                STRING_AGG(bi.seat_code, ', ' ORDER BY bi.seat_code) as seat_codes
         FROM bookings b
         JOIN users u ON b.user_id = u.id
         JOIN trips t ON b.trip_id = t.id
         JOIN routes r ON t.route_id = r.id
         LEFT JOIN booking_items bi ON b.id = bi.booking_id
         WHERE b.id=$1
         GROUP BY b.id, u.id, t.id, r.id`,
        [id]
      );

      if (fullBooking.rowCount > 0) {
        const bookingData = fullBooking.rows[0];
        console.log(`[admin.confirm] Found booking for email:`, {
          id: bookingData.id,
          email: bookingData.email,
          booking_code: bookingData.booking_code
        });

        const userData = {
          email: bookingData.email,
          name: bookingData.name,
          phone: bookingData.phone
        };
        const tripData = {
          origin: bookingData.origin,
          destination: bookingData.destination,
          departure_time: bookingData.departure_time,
          operator: bookingData.operator,
          bus_type: bookingData.bus_type
        };

        console.log(`[admin.confirm] Calling sendPaymentConfirmationEmail with email: ${bookingData.email}`);
        const emailResult = await sendPaymentConfirmationEmail(bookingData.email, bookingData, tripData, userData);
        console.log(`[admin.confirm] Email result:`, emailResult);

        if (emailResult && emailResult.success) {
          console.log(`✅ Confirmation email sent for booking ${id}`);
        } else {
          console.error(`❌ Email sending failed for booking ${id}:`, emailResult?.error || 'Unknown error');
        }
      } else {
        console.warn(`[admin.confirm] No booking data found for sending email (booking ${id})`);
      }
    } catch (emailError) {
      console.error(`❌ Failed to send email for booking ${id}:`, emailError.message || emailError);
      // Don't fail the request if email fails
    }

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
    const result = await db.query(
      "SELECT id, name, email, phone, role, status FROM users WHERE status != 'deleted' ORDER BY id ASC"
    );
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
    console.log(`[DELETE USER] Attempting to soft delete user ID: ${id}`);

    // Validate ID is numeric
    if (!id || isNaN(parseInt(id))) {
      return res.status(400).json({ message: "Invalid user ID" });
    }

    // Use soft delete: Set status to 'deleted' instead of permanent deletion
    // This preserves booking history while preventing login
    console.log(`[DELETE USER] Running UPDATE query for user ID: ${id}`);

    const result = await db.query(
      `UPDATE users
       SET status = 'deleted'
       WHERE id = $1
       RETURNING id, name, email, phone, role, status`,
      [parseInt(id)]
    );

    console.log(`[DELETE USER] Query result: ${result.rowCount} row(s) updated`);

    if (!result.rows.length) {
      console.log(`[DELETE USER] User not found with ID: ${id}`);
      return res.status(404).json({ message: "Người dùng không tìm thấy" });
    }

    console.log(`[DELETE USER] User ${id} successfully soft deleted`, result.rows[0]);
    res.json({
      message: "Xóa người dùng thành công (lịch sử vé vẫn được giữ lại)",
      user: result.rows[0]
    });
  } catch (err) {
    console.error(`[DELETE USER] Error deleting user ${id}:`, err.message);
    console.error(`[DELETE USER] Full error:`, err);
    res.status(500).json({
      message: "Lỗi khi xóa người dùng",
      error: err.message
    });
  }
});

// ============================================================// ROUTES: BÁO CÁO DOANH THU// ============================================================

router.get("/revenue", checkAdminRole, async (req, res) => {
    const { groupBy, route_id, trip_id, from_date, to_date, payment_method, operator } = req.query;

    let query = `
        SELECT
            %s AS group_key,
            COUNT(b.id) AS total_bookings,
            SUM(b.total_amount) AS total_revenue,
            SUM(b.seats_count) AS total_tickets
        FROM bookings b
        JOIN trips t ON b.trip_id = t.id
        JOIN routes r ON t.route_id = r.id
        WHERE b.status = 'confirmed' AND t.status != 'cancelled'
    `;
    const params = [];

    // ✅ Thêm filter theo payment_method
    if (payment_method && payment_method.toLowerCase() !== 'all') {
        params.push(payment_method);
        query += ` AND b.payment_method = $${params.length}`;
    }

    // ✅ Thêm filter theo operator
    if (operator && operator.toLowerCase() !== 'null') {
        params.push(operator);
        query += ` AND t.operator = $${params.length}`;
    }

    let groupByClause;
    let orderByClause;

    switch (groupBy) {
        case 'day':
        case 'date':
            groupByClause = "DATE(b.paid_at + INTERVAL '7 hours')";
            orderByClause = "group_key DESC";
            if (from_date) {
                params.push(from_date);
                query += ` AND DATE(b.paid_at + INTERVAL '7 hours') >= $${params.length}::date`;
            }
            if (to_date) {
                params.push(to_date);
                query += ` AND DATE(b.paid_at + INTERVAL '7 hours') < ($${params.length}::date + INTERVAL '1 day')`;
            }
            break;
        case 'month':
            groupByClause = "TO_CHAR(b.paid_at + INTERVAL '7 hours', 'YYYY-MM')";
            orderByClause = "group_key DESC";
            break;
        case 'year':
            groupByClause = "EXTRACT(YEAR FROM b.paid_at + INTERVAL '7 hours')";
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

// Báo cáo hoàn tiền (từ những bookings của trip bị hủy hoặc admin hủy vé đã thanh toán hoặc user hủy vé đã thanh toán)
router.get("/revenue/refunds", checkAdminRole, async (req, res) => {
    const { groupBy, route_id, trip_id, from_date, to_date, refundType, operator } = req.query;

    let query = `
        SELECT
            %s AS group_key,
            COUNT(b.id) AS total_bookings,
            SUM(CASE
                WHEN b.status = 'pending_refund' AND COALESCE(b.price_paid, 0) > 0 THEN COALESCE(b.price_paid, 0)
                WHEN b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled' THEN COALESCE(b.total_amount, 0)
                WHEN b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0 THEN COALESCE(b.price_paid, 0)
                ELSE 0
            END) AS refund_amount,
            SUM(b.seats_count) AS total_tickets,
            SUM(CASE WHEN b.status = 'pending_refund' THEN 1 ELSE 0 END) AS admin_cancelled_count,
            SUM(CASE WHEN b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled' THEN 1 ELSE 0 END) AS trip_cancelled_count,
            SUM(CASE WHEN b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0 THEN 1 ELSE 0 END) AS user_cancelled_count
        FROM bookings b
        LEFT JOIN trips t ON b.trip_id = t.id
        LEFT JOIN routes r ON t.route_id = r.id
        WHERE (
            (b.status = 'pending_refund' AND COALESCE(b.price_paid, 0) > 0)
            OR (b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled')
            OR (b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0)
        )
    `;
    const params = [];

    // Filter by refund type
    if (refundType === 'admin_cancelled') {
        query += ` AND b.status = 'pending_refund'`;
    } else if (refundType === 'trip_cancelled') {
        query += ` AND b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled'`;
    } else if (refundType === 'user_cancelled') {
        query += ` AND b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0`;
    }

    // ✅ Thêm filter theo operator
    if (operator && operator.toLowerCase() !== 'null') {
        params.push(operator);
        query += ` AND t.operator = $${params.length}`;
    }

    let groupByClause;
    let orderByClause;

    switch (groupBy) {
        case 'day':
        case 'date':
            groupByClause = "DATE(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours')";
            orderByClause = "group_key DESC";
            if (from_date) {
                params.push(from_date);
                query += ` AND DATE(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours') >= $${params.length}::date`;
            }
            if (to_date) {
                params.push(to_date);
                query += ` AND DATE(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours') < ($${params.length}::date + INTERVAL '1 day')`;
            }
            break;
        case 'month':
            groupByClause = "TO_CHAR(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours', 'YYYY-MM')";
            orderByClause = "group_key DESC";
            break;
        case 'year':
            groupByClause = "EXTRACT(YEAR FROM COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours')";
            orderByClause = "group_key DESC";
            break;
        case 'route':
            groupByClause = "r.id, r.origin, r.destination";
            orderByClause = "refund_amount DESC NULLS LAST";
            query = query.replace("%s", "COALESCE(r.id, 0) AS group_key, COALESCE(r.origin, 'N/A') AS origin, COALESCE(r.destination, 'N/A') AS destination");
            break;
        case 'trip':
            groupByClause = "COALESCE(t.id, 0), COALESCE(t.departure_time, NOW()), COALESCE(r.origin, 'N/A'), COALESCE(r.destination, 'N/A')";
            orderByClause = "refund_amount DESC NULLS LAST";
            query = query.replace("%s", "COALESCE(t.id, 0) AS group_key, COALESCE(t.departure_time, NOW()), COALESCE(r.origin, 'N/A'), COALESCE(r.destination, 'N/A')");
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
        console.log(`📊 [Refunds Report] refundType=${refundType}, groupBy=${groupBy}, Query: ${query}`);
        const result = await db.query(query, params);
        console.log(`📊 [Refunds Report] Found ${result.rows.length} rows`);
        res.json(result.rows);
    } catch (err) {
        console.error(`Error fetching refunds by ${groupBy}:`, err);
        res.status(500).json({ message: `Lỗi khi lấy báo cáo hoàn tiền theo ${groupBy}` });
    }
});

// 5. Chi tiết doanh thu
router.get("/revenue/details", checkAdminRole, async (req, res) => {
  const { group_by, value, payment_method, operator } = req.query;
  let sql = `
    SELECT
      b.id AS booking_id,
      u.name AS user_name,
      r.origin || ' - ' || r.destination AS route_info,
      t.departure_time,
      b.seats_count AS ticket_count,
      b.total_amount AS total_price,
      (b.paid_at + INTERVAL '7 hours') AS paid_at
    FROM bookings b
    JOIN users u ON u.id = b.user_id
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.status = 'confirmed' AND t.status != 'cancelled'
  `;
  const params = [];

  // ✅ Thêm filter theo payment_method
  if (payment_method && payment_method.toLowerCase() !== 'all') {
    params.push(payment_method);
    sql += ` AND b.payment_method = $${params.length}`;
  }

  // ✅ Thêm filter theo operator
  if (operator && operator.toLowerCase() !== 'null') {
    params.push(operator);
    sql += ` AND t.operator = $${params.length}`;
  }

  if (group_by === "day" || group_by === "date") {
    params.push(value);
    params.push(value);
    // Filter theo ngày đã cộng +7 tiếng (để hiển thị đúng theo múi giờ Việt Nam)
    sql += ` AND DATE(b.paid_at + INTERVAL '7 hours') >= $${params.length - 1}::date AND DATE(b.paid_at + INTERVAL '7 hours') < ($${params.length}::date + INTERVAL '1 day')`;
  } else if (group_by === "month") {
    params.push(value);
    sql += ` AND TO_CHAR(b.paid_at + INTERVAL '7 hours', 'YYYY-MM') = $${params.length}`;
  } else if (group_by === "year") {
    params.push(value);
    sql += ` AND EXTRACT(YEAR FROM b.paid_at + INTERVAL '7 hours') = $${params.length}`;
  } else if (group_by === "route") {
    // Assuming value is route_id
    params.push(value);
    sql += ` AND t.route_id = $${params.length}`;
  } else if (group_by === "trip") {
    params.push(value);
    sql += ` AND t.id = $${params.length}`;
  }

  sql += " ORDER BY COALESCE(b.paid_at, b.created_at) DESC";

  try {
    const result = await db.query(sql, params);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching revenue details:", err);
    res.status(500).json({ message: "Lỗi khi lấy chi tiết doanh thu" });
  }
});

// 6. Chi tiết hoàn tiền (từ những bookings của trip bị hủy hoặc admin hủy vé đã thanh toán hoặc user hủy vé đã thanh toán)
router.get("/revenue/refund-details", checkAdminRole, async (req, res) => {
  const { group_by, value, refundType, payment_method, operator } = req.query;
  let sql = `
    SELECT
      b.id AS booking_id,
      u.name AS user_name,
      COALESCE(r.origin || ' - ' || r.destination, 'Chuyến bị hủy') AS route_info,
      t.departure_time,
      b.seats_count AS ticket_count,
      CASE
        WHEN b.status = 'pending_refund' AND COALESCE(b.price_paid, 0) > 0 THEN COALESCE(b.price_paid, 0)
        WHEN b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled' THEN COALESCE(b.total_amount, 0)
        WHEN b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0 THEN COALESCE(b.price_paid, 0)
        ELSE 0
      END AS refund_amount,
      CASE
        WHEN b.status = 'pending_refund' THEN 'admin_cancelled'
        WHEN b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled' THEN 'trip_cancelled'
        WHEN b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0 THEN 'user_cancelled'
        ELSE 'unknown'
      END AS refund_type,
      (b.paid_at + INTERVAL '7 hours') AS paid_at
    FROM bookings b
    LEFT JOIN users u ON u.id = b.user_id
    LEFT JOIN trips t ON t.id = b.trip_id
    LEFT JOIN routes r ON r.id = t.route_id
    WHERE (
      (b.status = 'pending_refund' AND COALESCE(b.price_paid, 0) > 0)
      OR (b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled')
      OR (b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0)
    )
  `;
  const params = [];

  // Filter by refund type
  if (refundType === 'admin_cancelled') {
    sql += ` AND b.status = 'pending_refund'`;
  } else if (refundType === 'trip_cancelled') {
    sql += ` AND b.status = 'confirmed' AND COALESCE(t.status, '') = 'cancelled'`;
  } else if (refundType === 'user_cancelled') {
    sql += ` AND b.status = 'cancelled' AND COALESCE(b.price_paid, 0) > 0`;
  }

  // ✅ Thêm filter theo payment_method
  if (payment_method && payment_method.toLowerCase() !== 'all') {
    params.push(payment_method);
    sql += ` AND b.payment_method = $${params.length}`;
  }

  // ✅ Thêm filter theo operator
  if (operator && operator.toLowerCase() !== 'null') {
    params.push(operator);
    sql += ` AND t.operator = $${params.length}`;
  }

  if (group_by === "day" || group_by === "date") {
    params.push(value);
    params.push(value);
    // Include entire day from 00:00:00 to 23:59:59 in Vietnam timezone (với +7 tiếng)
    sql += ` AND DATE(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours') >= $${params.length - 1}::date AND DATE(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours') < ($${params.length}::date + INTERVAL '1 day')`;
  } else if (group_by === "month") {
    params.push(value);
    sql += ` AND TO_CHAR(COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours', 'YYYY-MM') = $${params.length}`;
  } else if (group_by === "year") {
    params.push(value);
    sql += ` AND EXTRACT(YEAR FROM COALESCE(b.paid_at, b.created_at) + INTERVAL '7 hours') = $${params.length}`;
  } else if (group_by === "route") {
    params.push(value);
    sql += ` AND t.route_id = $${params.length}`;
  } else if (group_by === "trip") {
    params.push(value);
    sql += ` AND t.id = $${params.length}`;
  }

  sql += " ORDER BY COALESCE(t.departure_time, b.created_at) DESC";

  try {
    console.log(`💸 [Refund Details] refundType=${refundType}, group_by=${group_by}, value=${value}`);
    const result = await db.query(sql, params);
    console.log(`💸 [Refund Details] Found ${result.rows.length} bookings`);
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching refund details:", err);
    res.status(500).json({ message: "Lỗi khi lấy chi tiết hoàn tiền" });
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

        // Send confirmation email
        try {
            console.log(`[admin.confirm-offline] Attempting to send email for booking ${id}`);
            const fullBooking = await db.query(
                `SELECT b.*, u.email, u.name, u.phone, r.origin, r.destination, t.departure_time, t.operator, t.bus_type,
                        STRING_AGG(bi.seat_code, ', ' ORDER BY bi.seat_code) as seat_codes
                 FROM bookings b
                 JOIN users u ON b.user_id = u.id
                 JOIN trips t ON b.trip_id = t.id
                 JOIN routes r ON t.route_id = r.id
                 LEFT JOIN booking_items bi ON b.id = bi.booking_id
                 WHERE b.id=$1
                 GROUP BY b.id, u.id, t.id, r.id`,
                [id]
            );

            if (fullBooking.rowCount > 0) {
                const bookingData = fullBooking.rows[0];
                console.log(`[admin.confirm-offline] Found booking:`, {
                    id: bookingData.id,
                    email: bookingData.email,
                    booking_code: bookingData.booking_code,
                    status: bookingData.status
                });

                const userData = {
                    email: bookingData.email,
                    name: bookingData.name,
                    phone: bookingData.phone
                };
                const tripData = {
                    origin: bookingData.origin,
                    destination: bookingData.destination,
                    departure_time: bookingData.departure_time,
                    operator: bookingData.operator,
                    bus_type: bookingData.bus_type
                };

                console.log(`[admin.confirm-offline] Calling sendPaymentConfirmationEmail with email: ${bookingData.email}`);
                const emailResult = await sendPaymentConfirmationEmail(bookingData.email, bookingData, tripData, userData);
                console.log(`[admin.confirm-offline] Email result:`, emailResult);

                if (emailResult.success) {
                    console.log(`✅ Confirmation email sent for booking ${id} (offline payment)`);
                } else {
                    console.error(`❌ Email sending failed for booking ${id}:`, emailResult.error);
                }
            } else {
                console.warn(`[admin.confirm-offline] No booking data found for sending email (booking ${id})`);
            }
        } catch (emailError) {
            console.error(`❌ Failed to send email for booking ${id}:`, emailError.message || emailError);
            // Don't fail the request if email fails
        }

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

// ============================================================
// TEST: Send payment confirmation email
// ============================================================
router.post("/test-send-email/:bookingId", checkAdminRole, async (req, res) => {
    const { bookingId } = req.params;

    try {
        console.log(`\n[TEST] Attempting to send payment email for booking ${bookingId}`);

        const fullBooking = await db.query(
            `SELECT b.*, u.email, u.name, u.phone, r.origin, r.destination, t.departure_time, t.operator, t.bus_type
             FROM bookings b
             JOIN users u ON b.user_id = u.id
             JOIN trips t ON b.trip_id = t.id
             JOIN routes r ON t.route_id = r.id
             WHERE b.id=$1`,
            [bookingId]
        );

        if (fullBooking.rowCount === 0) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        const bookingData = fullBooking.rows[0];
        console.log(`[TEST] Found booking:`, {
            id: bookingData.id,
            email: bookingData.email,
            name: bookingData.name,
            booking_code: bookingData.booking_code,
            status: bookingData.status
        });

        const userData = {
            email: bookingData.email,
            name: bookingData.name,
            phone: bookingData.phone
        };

        const tripData = {
            origin: bookingData.origin,
            destination: bookingData.destination,
            departure_time: bookingData.departure_time,
            operator: bookingData.operator,
            bus_type: bookingData.bus_type
        };

        console.log(`[TEST] Calling sendPaymentConfirmationEmail with email: ${bookingData.email}`);
        const result = await sendPaymentConfirmationEmail(bookingData.email, bookingData, tripData, userData);

        console.log(`[TEST] Email function result:`, result);

        res.json({
            success: true,
            message: 'Test email sent',
            booking_id: bookingId,
            email_sent_to: bookingData.email,
            booking_code: bookingData.booking_code
        });

    } catch (err) {
        console.error(`[TEST] Error sending test email:`, err);
        res.status(500).json({
            error: 'Failed to send test email',
            message: err.message || err,
            booking_id: bookingId
        });
    }
});

// ============================================================
// ROUTES: QUẢN LÝ DELETED USERS & BOOKING HISTORY
// ============================================================

// Get deleted users with their booking history
router.get("/deleted-users", checkAdminRole, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT
        u.id,
        u.name,
        u.email,
        u.phone,
        u.role,
        u.status,
        COUNT(b.id) as total_bookings,
        SUM(CASE WHEN b.status = 'confirmed' THEN 1 ELSE 0 END) as confirmed_bookings,
        MAX(b.created_at) as last_booking_date
       FROM users u
       LEFT JOIN bookings b ON u.id = b.user_id
       WHERE u.status = 'deleted'
       GROUP BY u.id, u.name, u.email, u.phone, u.role, u.status
       ORDER BY u.id DESC`
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching deleted users:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách người dùng đã xóa" });
  }
});

// Get booking history of a deleted user
router.get("/deleted-users/:id/bookings", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  try {
    const result = await db.query(
      `SELECT
        b.id,
        b.booking_code,
        b.user_id,
        u.name as user_name,
        u.email as user_email,
        t.id as trip_id,
        r.origin,
        r.destination,
        t.departure_date,
        t.departure_time,
        b.status,
        b.total_amount,
        b.created_at,
        b.updated_at
       FROM bookings b
       LEFT JOIN users u ON b.user_id = u.id
       JOIN trips t ON b.trip_id = t.id
       JOIN routes r ON t.route_id = r.id
       WHERE b.user_id = $1
       ORDER BY b.created_at DESC`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Error fetching deleted user's bookings:", err);
    res.status(500).json({ message: "Lỗi khi lấy lịch sử vé" });
  }
});

// Restore a deleted user (change status back to active/inactive)
router.put("/deleted-users/:id/restore", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const { newStatus = 'active' } = req.body;

  try {
    const result = await db.query(
      `UPDATE users
       SET status = $1
       WHERE id = $2 AND status = 'deleted'
       RETURNING id, name, email, phone, role, status`,
      [newStatus, id]
    );
    if (!result.rows.length) {
      return res.status(404).json({ message: "Người dùng không tìm thấy hoặc không ở trạng thái deleted" });
    }
    res.json({
      message: "Khôi phục người dùng thành công",
      user: result.rows[0]
    });
  } catch (err) {
    console.error("Error restoring deleted user:", err);
    res.status(500).json({ message: "Lỗi khi khôi phục người dùng" });
  }
});

module.exports = router;


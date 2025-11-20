// backend_api/routes/bookingRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

const beginTransaction = () => db.connect();

const commitAndRelease = async (client) => {
  try {
    await client.query("COMMIT");
  } finally {
    client.release();
  }
};

const rollbackAndRelease = async (client) => {
  try {
    await client.query("ROLLBACK");
  } finally {
    client.release();
  }
};

// ==========================================================
// ⭐ SỬA ĐỔI ROUTE ĐẶT VÉ ĐỂ HỖ TRỢ NHIỀU GHẾ (is_booked=1)
// ==========================================================
router.post("/bookings", async (req, res) => {
  // Chuyển từ 'seat_label' đơn lẻ sang 'seat_labels' là mảng
  const { user_id, trip_id, seat_labels } = req.body;

  // Kiểm tra đầu vào: seat_labels phải là mảng và không rỗng
  if (
    !user_id ||
    !trip_id ||
    !Array.isArray(seat_labels) ||
    seat_labels.length === 0
  )
    return res.status(400).json({ message: "Missing or invalid fields" });

  const client = await beginTransaction();
  try {
    await client.query("BEGIN");

    // 1. Khóa và lấy thông tin chuyến đi
    const tripResult = await client.query(
      "SELECT id, price, seats_available FROM trips WHERE id = $1 FOR UPDATE",
      [trip_id]
    );
    if (!tripResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(400).json({ message: "Trip not found" });
    }
    const tripPrice = tripResult.rows[0].price;
    const requiredSeats = seat_labels.length;

    // 2. Kiểm tra đủ chỗ trống
    if (tripResult.rows[0].seats_available < requiredSeats) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: "Not enough seats left for all selections" });
    }

    const createdBookingIds = [];

    // 3. LẶP QUA TỪNG GHẾ ĐÃ CHỌN
    for (const label of seat_labels) {

      // a. Khóa và kiểm tra ghế
      const seatResult = await client.query(
        "SELECT id, is_booked FROM seats WHERE trip_id=$1 AND label=$2 FOR UPDATE",
        [trip_id, label]
      );
      if (!seatResult.rowCount) {
        await rollbackAndRelease(client);
        return res.status(400).json({ message: `Seat ${label} not found` });
      }
      // ⭐ Sửa lỗi: Kiểm tra giá trị Integer (1) thay vì Boolean (TRUE)
      if (seatResult.rows[0].is_booked === 1) {
        await rollbackAndRelease(client);
        return res.status(409).json({ message: `Seat ${label} already booked` });
      }

      // b. Tạo booking cho từng ghế
      const bookingInsert = await client.query(
        `INSERT INTO bookings (user_id, trip_id, seat_label, price_paid, status, created_at)
         VALUES ($1, $2, $3, $4, 'pending', NOW())
         RETURNING id`,
        [user_id, trip_id, label, tripPrice]
      );
      const bookingId = bookingInsert.rows[0].id;
      createdBookingIds.push(bookingId);

      // c. Cập nhật trạng thái ghế
      await client.query(
        "UPDATE seats SET is_booked=1, booking_id=$1 WHERE id=$2", // ⭐ Dùng '1'
        [bookingId, seatResult.rows[0].id]
      );
    }

    // 4. Cập nhật số ghế trống (chỉ 1 lần)
    await client.query(
      "UPDATE trips SET seats_available = seats_available - $1 WHERE id=$2",
      [requiredSeats, trip_id]
    );

    await commitAndRelease(client);

    // Trả về danh sách các booking ID đã tạo
    res.json({
        message: "Booked successfully",
        booking_ids: createdBookingIds,
        total_bookings: requiredSeats
    });

  } catch (err) {
    console.error("Booking failed:", err.message || err);
    await rollbackAndRelease(client);
    // Trả về lỗi 500 nếu có lỗi xảy ra trong quá trình lặp
    res.status(500).json({ message: "Booking failed due to a server error" });
  }
});

// ==========================================================
// ⭐ ROUTE HỦY (Cần sửa lỗi is_booked)
// ==========================================================
router.post("/bookings/:id/cancel", async (req, res) => {
  const { id } = req.params;
  const client = await beginTransaction();
  try {
    await client.query("BEGIN");

    const bookingResult = await client.query(
      "SELECT * FROM bookings WHERE id=$1 FOR UPDATE",
      [id]
    );
    if (!bookingResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: "Booking not found" });
    }
    if (!["confirmed", "pending"].includes(bookingResult.rows[0].status)) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: "Cannot cancel" });
    }

    await client.query("UPDATE bookings SET status='cancelled' WHERE id=$1", [id]);
    await client.query(
      "UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=$1 AND label=$2", // ⭐ Dùng '0' thay vì FALSE
      [bookingResult.rows[0].trip_id, bookingResult.rows[0].seat_label]
    );
    await client.query(
      "UPDATE trips SET seats_available = seats_available + 1 WHERE id=$1",
      [bookingResult.rows[0].trip_id]
    );

    await commitAndRelease(client);
    res.json({ message: "Cancelled" });
  } catch (err) {
    console.error("Cancel booking failed:", err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: "Cancel booking failed" });
  }
});

router.get("/bookings/my", async (req, res) => {
  const { user_id } = req.query;
  if (!user_id) return res.status(400).json({ message: "Missing user_id" });
  const sql = `
    SELECT b.id, b.seat_label, b.status, b.price_paid, b.created_at, b.payment_method, b.qr_code,
           t.departure_time, t.arrival_time, t.operator, t.bus_type,
           r.origin, r.destination, r.distance_km AS distance, r.duration_min AS duration
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.user_id=$1
    ORDER BY b.created_at DESC
  `;
  try {
    const { rows } = await db.query(sql, [user_id]);
    res.json(rows);
  } catch (err) {
    console.error("Failed to fetch my bookings:", err);
    res.status(500).json({ message: "Failed to fetch bookings" });
  }
});

router.post("/bookings/:id/payment", async (req, res) => {
  const { id } = req.params;
  const { payment_method } = req.body;
  if (!payment_method) {
    return res.status(400).json({ message: "Missing payment_method" });
  }

  const client = await beginTransaction();
  try {
    await client.query("BEGIN");

    const bookingResult = await client.query(
      "SELECT * FROM bookings WHERE id=$1 FOR UPDATE",
      [id]
    );
    if (!bookingResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: "Booking not found" });
    }
    if (bookingResult.rows[0].status !== "pending") {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: "Booking already processed" });
    }

    const qrData = `BOOKING-${id}-${Date.now()}`;
    await client.query(
      "UPDATE bookings SET status='confirmed', payment_method=$1, qr_code=$2, payment_time=NOW() WHERE id=$3",
      [payment_method, qrData, id]
    );

    await commitAndRelease(client);
    res.json({
      message: "Payment confirmed",
      qr_code: qrData,
      booking_id: id
    });
  } catch (err) {
    console.error("Payment confirmation failed:", err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: "Payment confirmation failed" });
  }
});

router.get("/bookings/:id", async (req, res) => {
  const { id } = req.params;
  const sql = `
    SELECT b.id, b.user_id, b.trip_id, b.seat_label, b.price_paid, b.status, b.created_at, b.payment_method, b.payment_time, b.qr_code,
           t.departure_time, t.arrival_time, t.operator, t.bus_type,
           r.origin, r.destination, r.distance_km AS distance, r.duration_min AS duration
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.id=$1
  `;
  try {
    const { rows } = await db.query(sql, [id]);
    if (!rows.length) return res.status(404).json({ message: "Booking not found" });
    res.json(rows[0]);
  } catch (err) {
    console.error("Failed to fetch booking:", err);
    res.status(500).json({ message: "Failed to fetch booking" });
  }
});

module.exports = router;
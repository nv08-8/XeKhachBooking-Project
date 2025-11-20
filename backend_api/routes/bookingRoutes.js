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

router.post("/bookings", async (req, res) => {
  const { user_id, trip_id, seat_label } = req.body;
  if (!user_id || !trip_id || !seat_label)
    return res.status(400).json({ message: "Missing fields" });

  const client = await beginTransaction();
  try {
    await client.query("BEGIN");

    const tripResult = await client.query(
      "SELECT id, price, seats_available FROM trips WHERE id = $1 FOR UPDATE",
      [trip_id]
    );
    if (!tripResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(400).json({ message: "Trip not found" });
    }
    if (tripResult.rows[0].seats_available <= 0) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: "No seats left" });
    }

    const seatResult = await client.query(
      "SELECT id, is_booked FROM seats WHERE trip_id=$1 AND label=$2 FOR UPDATE",
      [trip_id, seat_label]
    );
    if (!seatResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(400).json({ message: "Seat not found" });
    }
    if (seatResult.rows[0].is_booked) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: "Seat already booked" });
    }

    const bookingInsert = await client.query(
      `INSERT INTO bookings (user_id, trip_id, seat_label, price_paid, status, created_at)
       VALUES ($1, $2, $3, $4, 'pending', NOW())
       RETURNING id`,
      [user_id, trip_id, seat_label, tripResult.rows[0].price]
    );
    const bookingId = bookingInsert.rows[0].id;

    await client.query(
      "UPDATE seats SET is_booked=TRUE, booking_id=$1 WHERE id=$2",
      [bookingId, seatResult.rows[0].id]
    );

    await client.query(
      "UPDATE trips SET seats_available = seats_available - 1 WHERE id=$1",
      [trip_id]
    );

    await commitAndRelease(client);
    res.json({ message: "Booked successfully", booking_id: bookingId });
  } catch (err) {
    console.error("Booking failed:", err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: "Booking failed" });
  }
});

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
      "UPDATE seats SET is_booked=FALSE, booking_id=NULL WHERE trip_id=$1 AND label=$2",
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

// API: Lấy danh sách ghế (Tự động tạo nếu chưa có)
router.get("/seats/:tripId", async (req, res) => {
  const { tripId } = req.params;
  const tripIdInt = parseInt(tripId, 10);

  if (isNaN(tripIdInt)) {
    return res.status(400).json({ message: "Trip ID phải là số" });
  }

  try {
    console.log(`Đang tìm ghế cho Trip ID: ${tripIdInt}`);

    // 1. Lấy danh sách ghế hiện có
    let sql = `
      SELECT id, trip_id, label, is_booked
      FROM seats
      WHERE trip_id = $1
      ORDER BY id ASC
    `;
    let { rows } = await db.query(sql, [tripIdInt]);

    // 2. Nếu chưa có ghế nào -> TỰ ĐỘNG TẠO 35 GHẾ CHO CHUYẾN NÀY
    if (rows.length === 0) {
      console.log(`Chưa có ghế cho Trip ${tripIdInt}. Đang tạo mới...`);

      // Tạo 35 ghế: A01 -> A35
      for (let i = 1; i <= 35; i++) {
        const label = `A${i.toString().padStart(2, '0')}`;

        // Lưu ý: is_booked để là 0 (vì database của bạn dùng integer 0/1)
        await db.query(
          "INSERT INTO seats (trip_id, label, is_booked, type) VALUES ($1, $2, 0, 'seat')",
          [tripIdInt, label]
        );
      }

      // Query lại lần nữa để lấy danh sách vừa tạo
      const result = await db.query(sql, [tripIdInt]);
      rows = result.rows;
    }

    // 3. Chuyển đổi dữ liệu để App Android hiểu (0 -> false, 1 -> true)
    // App Android thường mong đợi boolean cho trường isBooked
    const formattedRows = rows.map(seat => ({
        ...seat,
        is_booked: seat.is_booked === 1 // Chuyển 1 thành true, 0 thành false
    }));

    console.log(`Trả về ${formattedRows.length} ghế.`);
    res.json(formattedRows);

  } catch (err) {
    console.error("Lỗi lấy danh sách ghế:", err);
    res.status(500).json({ message: "Lỗi server", error: err.message });
  }
});

module.exports = router;

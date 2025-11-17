// backend_api/routes/bookingRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// POST /bookings  { user_id, trip_id, seat_label }
router.post("/bookings", (req, res) => {
  const { user_id, trip_id, seat_label } = req.body;
  if (!user_id || !trip_id || !seat_label)
    return res.status(400).json({ message: "Missing fields" });

  db.getConnection((err, conn) => {
    if (err) return res.status(500).json({ message: err.message });

    conn.beginTransaction(txErr => {
      if (txErr) { conn.release(); return res.status(500).json({ message: txErr.message }); }

      conn.query(
        "SELECT t.price, t.seats_available FROM trips t WHERE t.id = ? FOR UPDATE",
        [trip_id],
        (errTrip, tripRows) => {
          if (errTrip || !tripRows.length) {
            conn.rollback(() => { conn.release(); });
            return res.status(400).json({ message: "Trip not found" });
          }
          if (tripRows[0].seats_available <= 0) {
            conn.rollback(() => { conn.release(); });
            return res.status(409).json({ message: "No seats left" });
          }

          conn.query(
            "SELECT id, is_booked FROM seats WHERE trip_id=? AND label=? FOR UPDATE",
            [trip_id, seat_label],
            (errSeat, seatRows) => {
              if (errSeat || !seatRows.length) {
                conn.rollback(() => { conn.release(); });
                return res.status(400).json({ message: "Seat not found" });
              }
              if (seatRows[0].is_booked) {
                conn.rollback(() => { conn.release(); });
                return res.status(409).json({ message: "Seat already booked" });
              }

              conn.query(
                "INSERT INTO bookings (user_id, trip_id, seat_label, price_paid, status, created_at) VALUES (?, ?, ?, ?, 'pending', NOW())",
                [user_id, trip_id, seat_label, tripRows[0].price],
                (errBook, result) => {
                  if (errBook) {
                    conn.rollback(() => { conn.release(); });
                    return res.status(500).json({ message: errBook.message });
                  }
                  const bookingId = result.insertId;

                  conn.query(
                    "UPDATE seats SET is_booked=1, booking_id=? WHERE id=?",
                    [bookingId, seatRows[0].id],
                    (errSeatUpd) => {
                      if (errSeatUpd) {
                        conn.rollback(() => { conn.release(); });
                        return res.status(500).json({ message: errSeatUpd.message });
                      }

                      conn.query(
                        "UPDATE trips SET seats_available = seats_available - 1 WHERE id=?",
                        [trip_id],
                        (errTripUpd) => {
                          if (errTripUpd) {
                            conn.rollback(() => { conn.release(); });
                            return res.status(500).json({ message: errTripUpd.message });
                          }

                          conn.commit(commitErr => {
                            conn.release();
                            if (commitErr) return res.status(500).json({ message: commitErr.message });
                            res.json({ message: "Booked successfully", booking_id: bookingId });
                          });
                        }
                      );
                    }
                  );
                }
              );
            }
          );
        }
      );
    });
  });
});

// POST /bookings/:id/cancel
router.post("/bookings/:id/cancel", (req, res) => {
  const { id } = req.params;
  db.getConnection((err, conn) => {
    if (err) return res.status(500).json({ message: err.message });

    conn.beginTransaction(txErr => {
      if (txErr) { conn.release(); return res.status(500).json({ message: txErr.message }); }

      conn.query("SELECT * FROM bookings WHERE id=? FOR UPDATE", [id], (errB, rowsB) => {
        if (errB || !rowsB.length) {
          conn.rollback(() => { conn.release(); });
          return res.status(404).json({ message: "Booking not found" });
        }
        if (rowsB[0].status !== "confirmed" && rowsB[0].status !== "pending") {
          conn.rollback(() => { conn.release(); });
          return res.status(409).json({ message: "Cannot cancel" });
        }

        conn.query("UPDATE bookings SET status='cancelled' WHERE id=?", [id], (errUpd) => {
          if (errUpd) {
            conn.rollback(() => { conn.release(); });
            return res.status(500).json({ message: errUpd.message });
          }

            conn.query("UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=? AND label=?",
              [rowsB[0].trip_id, rowsB[0].seat_label],
              (errSeat) => {
                if (errSeat) {
                  conn.rollback(() => { conn.release(); });
                  return res.status(500).json({ message: errSeat.message });
                }

                conn.query("UPDATE trips SET seats_available = seats_available + 1 WHERE id=?",
                  [rowsB[0].trip_id],
                  (errTrip) => {
                    if (errTrip) {
                      conn.rollback(() => { conn.release(); });
                      return res.status(500).json({ message: errTrip.message });
                    }
                    conn.commit(commitErr => {
                      conn.release();
                      if (commitErr) return res.status(500).json({ message: commitErr.message });
                      res.json({ message: "Cancelled" });
                    });
                  });
              });
        });
      });
    });
  });
});

// GET /bookings/my?user_id=...
router.get("/bookings/my", (req, res) => {
  const { user_id } = req.query;
  if (!user_id) return res.status(400).json({ message: "Missing user_id" });
  const sql = `
    SELECT b.id, b.seat_label, b.status, b.price_paid, b.created_at, b.payment_method, b.qr_code,
           t.departure_time, t.arrival_time, t.operator, t.bus_type,
           r.origin, r.destination, r.distance_km AS distance, r.duration_min AS duration
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.user_id=?
    ORDER BY b.created_at DESC
  `;
  db.query(sql, [user_id], (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    res.json(rows);
  });
});

// POST /bookings/:id/payment - Confirm payment and generate QR code
router.post("/bookings/:id/payment", (req, res) => {
  const { id } = req.params;
  const { payment_method } = req.body; // 'momo' or 'bank'

  if (!payment_method) {
    return res.status(400).json({ message: "Missing payment_method" });
  }

  db.getConnection((err, conn) => {
    if (err) return res.status(500).json({ message: err.message });

    conn.beginTransaction(txErr => {
      if (txErr) {
        conn.release();
        return res.status(500).json({ message: txErr.message });
      }

      conn.query(
        "SELECT * FROM bookings WHERE id=? FOR UPDATE",
        [id],
        (errBook, bookRows) => {
          if (errBook || !bookRows.length) {
            conn.rollback(() => { conn.release(); });
            return res.status(404).json({ message: "Booking not found" });
          }

          if (bookRows[0].status !== "pending") {
            conn.rollback(() => { conn.release(); });
            return res.status(409).json({ message: "Booking already processed" });
          }

          // Generate QR code data (booking ID + timestamp)
          const qrData = `BOOKING-${id}-${Date.now()}`;

          conn.query(
            "UPDATE bookings SET status='confirmed', payment_method=?, qr_code=?, payment_time=NOW() WHERE id=?",
            [payment_method, qrData, id],
            (errUpd) => {
              if (errUpd) {
                conn.rollback(() => { conn.release(); });
                return res.status(500).json({ message: errUpd.message });
              }

              conn.commit(commitErr => {
                conn.release();
                if (commitErr) {
                  return res.status(500).json({ message: commitErr.message });
                }
                res.json({
                  message: "Payment confirmed",
                  qr_code: qrData,
                  booking_id: id
                });
              });
            }
          );
        }
      );
    });
  });
});

// GET /bookings/:id - Get booking details
router.get("/bookings/:id", (req, res) => {
  const { id } = req.params;
  const sql = `
    SELECT b.*,
           t.departure_time, t.arrival_time, t.operator, t.bus_type,
           r.origin, r.destination, r.distance_km AS distance, r.duration_min AS duration
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE b.id=?
  `;
  db.query(sql, [id], (err, rows) => {
    if (err) return res.status(500).json({ message: err.message });
    if (!rows.length) return res.status(404).json({ message: "Booking not found" });
    res.json(rows[0]);
  });
});

module.exports = router;

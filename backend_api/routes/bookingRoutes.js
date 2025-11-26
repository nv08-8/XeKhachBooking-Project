const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateAndCacheSeats } = require("../utils/seatGenerator");

// Helper functions for transaction management
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

// POST /bookings - Create a new booking with multiple seats
router.post("/bookings", async (req, res) => {
  const { user_id, trip_id, seat_labels, promotion_code, metadata, pickup_stop_id, dropoff_stop_id } = req.body;

  if (!user_id || !trip_id || !Array.isArray(seat_labels) || seat_labels.length === 0 || !pickup_stop_id || !dropoff_stop_id) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  const client = await beginTransaction();
  try {
    await client.query('BEGIN');

    // Ensure seats are generated before attempting to book, within the same transaction
    await generateAndCacheSeats(client, trip_id);

    const tripResult = await client.query('SELECT price, seats_available FROM trips WHERE id=$1 FOR UPDATE', [trip_id]);
    if (!tripResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Trip not found' });
    }

    const tripPrice = parseFloat(tripResult.rows[0].price) || 0;
    const requiredSeats = seat_labels.length;

    if (tripResult.rows[0].seats_available < requiredSeats) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: 'Not enough seats available' });
    }

    for (const label of seat_labels) {
      const seatRes = await client.query('SELECT id, is_booked FROM seats WHERE trip_id=$1 AND label=$2 FOR UPDATE', [trip_id, label]);
      if (!seatRes.rowCount || seatRes.rows[0].is_booked) {
        await rollbackAndRelease(client);
        return res.status(409).json({ message: `Seat ${label} is not available` });
      }
    }

    const totalAmount = Number((tripPrice * requiredSeats).toFixed(2));
    
    const bookingInsert = await client.query(
      `INSERT INTO bookings (user_id, trip_id, total_amount, seats_count, promotion_code, status, metadata, pickup_stop_id, dropoff_stop_id) 
       VALUES ($1, $2, $3, $4, $5, 'pending', $6, $7, $8) RETURNING id`,
      [user_id, trip_id, totalAmount, requiredSeats, promotion_code || null, metadata ? JSON.stringify(metadata) : null, pickup_stop_id, dropoff_stop_id]
    );
    const bookingId = bookingInsert.rows[0].id;
    
    const bookingIds = [bookingId];
    for (const label of seat_labels) {
      await client.query(
        `INSERT INTO booking_items (booking_id, seat_code, price) VALUES ($1, $2, $3)`,
        [bookingId, label, tripPrice]
      );
      await client.query(
        'UPDATE seats SET is_booked=1, booking_id=$1 WHERE trip_id=$2 AND label=$3',
        [bookingId, trip_id, label]
      );
    }

    await client.query('UPDATE trips SET seats_available = seats_available - $1 WHERE id=$2', [requiredSeats, trip_id]);

    await commitAndRelease(client);
    res.json({ message: 'Booking created successfully', booking_ids: bookingIds, total_amount: totalAmount });

  } catch (err) {
    console.error('Booking failed:', err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Booking failed due to a server error' });
  }
});

// GET /bookings/my - Get user's bookings
router.get('/bookings/my', async (req, res) => {
  const { user_id } = req.query;
  if (!user_id) return res.status(400).json({ message: 'Missing user_id' });
  
  const sql = `
    SELECT b.id, b.status, b.price_paid, b.created_at, b.total_amount,
           t.departure_time, t.arrival_time, t.operator,
           r.origin, r.destination,
           pickup_stop.name AS pickup_location,
           dropoff_stop.name AS dropoff_location,
           COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) AS seat_labels
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    LEFT JOIN booking_items bi ON bi.booking_id = b.id
    LEFT JOIN route_stops pickup_stop ON pickup_stop.id = b.pickup_stop_id
    LEFT JOIN route_stops dropoff_stop ON dropoff_stop.id = b.dropoff_stop_id
    WHERE b.user_id = $1
    GROUP BY b.id, t.id, r.id, pickup_stop.id, dropoff_stop.id
    ORDER BY b.created_at DESC
  `;
  try {
    const { rows } = await db.query(sql, [user_id]);
    res.json(rows);
  } catch (err) {
    console.error('Failed to fetch bookings:', err);
    res.status(500).json({ message: 'Failed to fetch bookings' });
  }
});

// GET /bookings/:id - Get specific booking details
router.get('/bookings/:id', async (req, res) => {
  const { id } = req.params;
  const sql = `
    SELECT b.*, 
           t.departure_time, t.arrival_time, t.operator, 
           r.origin, r.destination, 
           u.name AS passenger_name, u.phone AS passenger_phone,
           pickup_stop.name AS pickup_location, pickup_stop.address AS pickup_address,
           dropoff_stop.name AS dropoff_location, dropoff_stop.address AS dropoff_address,
           COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) AS seat_labels
    FROM bookings b
    JOIN trips t ON t.id = b.trip_id
    JOIN routes r ON r.id = t.route_id
    JOIN users u ON u.id = b.user_id
    LEFT JOIN booking_items bi ON bi.booking_id = b.id
    LEFT JOIN route_stops pickup_stop ON pickup_stop.id = b.pickup_stop_id
    LEFT JOIN route_stops dropoff_stop ON dropoff_stop.id = b.dropoff_stop_id
    WHERE b.id=$1
    GROUP BY b.id, t.id, r.id, u.id, pickup_stop.id, dropoff_stop.id
  `;
  try {
    const { rows } = await db.query(sql, [id]);
    if (!rows.length) return res.status(404).json({ message: 'Booking not found' });
    res.json(rows[0]);
  } catch (err) {
    console.error('Failed to fetch booking:', err);
    res.status(500).json({ message: 'Failed to fetch booking' });
  }
});

// POST /bookings/:id/cancel
router.post('/bookings/:id/cancel', async (req, res) => {
  const { id } = req.params;
  const client = await beginTransaction();
  try {
    await client.query('BEGIN');
    const bookingRes = await client.query(
      `SELECT b.id, b.trip_id, b.status, b.total_amount, b.price_paid, t.departure_time
       FROM bookings b JOIN trips t ON t.id = b.trip_id WHERE b.id=$1 FOR UPDATE`,
      [id]
    );
    if (!bookingRes.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Booking not found' });
    }
    const booking = bookingRes.rows[0];
    if (!['confirmed', 'pending'].includes(booking.status)) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: 'Cannot cancel this booking' });
    }

    const hoursUntilDeparture = (new Date(booking.departure_time) - new Date()) / 36e5;
    if (hoursUntilDeparture <= 2) {
        await rollbackAndRelease(client);
        return res.status(409).json({ message: 'Cannot cancel within 2 hours of departure', canCancel: false });
    }

    let refundPercentage = 0;
    if (hoursUntilDeparture >= 24) refundPercentage = 90;
    else if (hoursUntilDeparture >= 12) refundPercentage = 70;
    else if (hoursUntilDeparture >= 6) refundPercentage = 50;

    const baseForRefund = Number(booking.price_paid) > 0 ? Number(booking.price_paid) : Number(booking.total_amount);
    const refundAmount = Math.floor((baseForRefund * refundPercentage) / 100);
    const newStatus = refundAmount > 0 ? 'pending_refund' : 'cancelled';

    await client.query(
        'UPDATE bookings SET status=$1, cancelled_at=NOW(), metadata = metadata || $2::jsonb WHERE id=$3',
        [newStatus, JSON.stringify({ cancellation: { refundAmount, refundPercentage } }), id]
    );

    const { rowCount: releasedCount } = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1', [id]);
    if (releasedCount > 0) {
      await client.query('UPDATE trips SET seats_available = seats_available + $1 WHERE id=$2', [releasedCount, booking.trip_id]);
    }

    await commitAndRelease(client);
    res.json({ message: 'Booking cancelled successfully', status: newStatus, refundAmount });
  } catch (err) {
    console.error('Cancel booking failed:', err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Could not cancel booking' });
  }
});

module.exports = router;

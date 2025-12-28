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
  const { user_id, trip_id, seat_labels, promotion_code, metadata, pickup_stop_id, dropoff_stop_id, payment_method, passenger_name, passenger_phone, passenger_email } = req.body;

  if (!user_id || !trip_id || !Array.isArray(seat_labels) || seat_labels.length === 0 || !pickup_stop_id || !dropoff_stop_id) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  // ✅ Determine payment method (default to 'offline' if not provided for backwards compatibility)
  const finalPaymentMethod = payment_method || 'offline';
  const finalPassengerName = passenger_name || '';
  const finalPassengerPhone = passenger_phone || '';
  const finalPassengerEmail = passenger_email || '';
  console.log(`Creating booking with payment_method: ${finalPaymentMethod}, passenger: ${finalPassengerName}`);

  const client = await beginTransaction();
  try {
    await client.query('BEGIN');

    // Ensure seats are generated before attempting to book, within the same transaction
    await generateAndCacheSeats(client, trip_id);

    // Check if trip exists and hasn't departed yet
    const tripResult = await client.query(
      'SELECT t.price, t.seats_available, t.departure_time, t.arrival_time FROM trips t WHERE t.id=$1 FOR UPDATE',
      [trip_id]
    );
    if (!tripResult.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Trip not found' });
    }

    const trip = tripResult.rows[0];
    const tripPrice = parseFloat(trip.price) || 0;
    const requiredSeats = seat_labels.length;

    // Prevent booking if trip has already departed
    const now = new Date();
    const departureTime = new Date(trip.departure_time);
    if (departureTime <= now) {
      await rollbackAndRelease(client);
      return res.status(400).json({
        message: 'Cannot book this trip - it has already departed',
        departure_time: trip.departure_time
      });
    }

    if (trip.seats_available < requiredSeats) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: 'Not enough seats available' });
    }

    for (const label of seat_labels) {
      // Try to lock the seat row if it exists
      let seatRes = await client.query('SELECT id, is_booked FROM seats WHERE trip_id=$1 AND label=$2 FOR UPDATE', [trip_id, label]);
      if (!seatRes.rowCount) {
        // Seat row missing (no seat records generated). Create the seat record on-demand.
        // Use ON CONFLICT DO NOTHING to avoid race errors if another tx creates it concurrently.
        await client.query(
          'INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1, $2, $3, 0) ON CONFLICT (trip_id, label) DO NOTHING',
          [trip_id, label, 'seat']
        );
        // Re-select with FOR UPDATE to lock the row we just created (or the row created by another tx)
        seatRes = await client.query('SELECT id, is_booked FROM seats WHERE trip_id=$1 AND label=$2 FOR UPDATE', [trip_id, label]);
      }

      if (!seatRes.rowCount || seatRes.rows[0].is_booked) {
        await rollbackAndRelease(client);
        return res.status(409).json({ message: `Seat ${label} is not available` });
      }
    }

    const totalAmount = Number((tripPrice * requiredSeats).toFixed(2));

    // If a promotion_code is provided, re-validate it server-side and compute discount
    let finalAmount = totalAmount;
    if (promotion_code) {
      try {
        const promoRes = await client.query(
          `SELECT id, code, discount_type, discount_value, min_price, max_discount, start_date, end_date, status FROM promotions WHERE code=$1 LIMIT 1`,
          [promotion_code]
        );
        if (promoRes.rowCount) {
          const promo = promoRes.rows[0];
          const nowRes = await client.query('SELECT NOW() as now');
          const now = nowRes.rows && nowRes.rows[0] && nowRes.rows[0].now ? new Date(nowRes.rows[0].now) : new Date();
          // Check status and dates
          if (promo.status === 'active' && (!promo.start_date || new Date(promo.start_date) <= now) && (!promo.end_date || new Date(promo.end_date) >= now)) {
            const minPrice = Number(promo.min_price || 0);
            if (!minPrice || totalAmount >= minPrice) {
              // compute discount
              const type = (promo.discount_type || '').toLowerCase();
              const value = Number(promo.discount_value || 0);
              let rawDiscount = 0;
              if (type === 'percent' || type === 'percentage') rawDiscount = totalAmount * (value/100);
              else rawDiscount = value;
              const maxDiscount = Number(promo.max_discount || 0);
              let discount = rawDiscount;
              if (maxDiscount > 0) discount = Math.min(discount, maxDiscount);
              discount = Math.max(0, Math.min(discount, totalAmount));
              finalAmount = Number((totalAmount - discount).toFixed(2));
            }
          }
        }
      } catch (e) {
        console.error('Failed to validate promotion while creating booking:', e);
      }
    }

    // Build passenger_info JSONB object
    const passengerInfo = {};
    if (finalPassengerName) passengerInfo.name = finalPassengerName;
    if (finalPassengerPhone) passengerInfo.phone = finalPassengerPhone;
    if (finalPassengerEmail) passengerInfo.email = finalPassengerEmail;
    const passengerInfoJson = Object.keys(passengerInfo).length > 0 ? JSON.stringify(passengerInfo) : null;

    const bookingInsert = await client.query(
      `INSERT INTO bookings (user_id, trip_id, total_amount, seats_count, promotion_code, status, metadata, pickup_stop_id, dropoff_stop_id, payment_method, passenger_info)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING id`,
      [user_id, trip_id, finalAmount, requiredSeats, promotion_code || null, 'pending', metadata ? JSON.stringify(metadata) : null, pickup_stop_id, dropoff_stop_id, finalPaymentMethod, passengerInfoJson]
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
    res.json({ message: 'Booking created successfully', booking_ids: bookingIds, total_amount: finalAmount });

  } catch (err) {
    console.error('Booking failed:', err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Booking failed due to a server error' });
  }
});

// GET /bookings/my - Get user's bookings
// Query params:
//   - user_id (required): user ID
//   - tab (optional): 'current' | 'completed'
//     - 'current': all bookings in last 3 months (all statuses)
//     - 'completed': bookings with arrival_time < NOW() and status = 'completed' (excludes cancelled)
router.get('/bookings/my', async (req, res) => {
  const { user_id, tab } = req.query;
  console.log(`\n📋 [GET /api/bookings/my] Request from user_id: ${user_id}, tab: ${tab || 'all'}`);

  if (!user_id) return res.status(400).json({ message: 'Missing user_id' });
  
  // Build WHERE clause based on tab parameter
  let whereClause = 'WHERE b.user_id = $1';

  if (tab === 'current') {
    // Tab "Hiện tại": All bookings in last 3 months (all statuses including cancelled, pending, etc.)
    whereClause += ` AND b.created_at >= NOW() - INTERVAL '3 months'`;
  } else if (tab === 'completed') {
    // Tab "Đã đi": Only completed trips (arrival_time passed and status = 'completed')
    // Excludes cancelled bookings
    whereClause += ` AND t.arrival_time < NOW() AND b.status = 'completed'`;
  }
  // If no tab specified, return all bookings (backwards compatibility)

  const sql = `
    SELECT b.id, b.status, b.price_paid, b.created_at, b.total_amount, b.payment_method,
           t.departure_time, t.arrival_time, t.operator, t.bus_type,
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
    ${whereClause}
    GROUP BY b.id, t.id, r.id, pickup_stop.id, dropoff_stop.id
    ORDER BY b.created_at DESC
  `;
  try {
    const { rows } = await db.query(sql, [user_id]);
    console.log(`   ✅ Found ${rows.length} bookings for user ${user_id} (tab: ${tab || 'all'})`);
    if (rows.length > 0) {
      console.log(`   First booking: #${rows[0].id}, status: ${rows[0].status}, route: ${rows[0].origin} → ${rows[0].destination}`);
    }
    res.json(rows);
  } catch (err) {
    console.error('❌ Failed to fetch bookings:', err);
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

    // If there's a recorded paid amount, compute refund; otherwise (unpaid/pending) mark as cancelled
    let refundAmount = 0;
    let newStatus = 'cancelled';
    if (Number(booking.price_paid) > 0) {
        const baseForRefund = Number(booking.price_paid);
        refundAmount = Math.floor((baseForRefund * refundPercentage) / 100);
        newStatus = refundAmount > 0 ? 'pending_refund' : 'cancelled';
    } else {
        // unpaid booking: cancel without refund
        refundAmount = 0;
        newStatus = 'cancelled';
    }

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

// POST /bookings/:id/payment - confirm payment for a booking (e.g., card)
router.post('/bookings/:id/payment', async (req, res) => {
  const { id } = req.params;
  const { payment_method } = req.body || {};
  const client = await beginTransaction();
  try {
    await client.query('BEGIN');
    const bookingRes = await client.query(
      `SELECT b.id, b.trip_id, b.status, b.total_amount, b.price_paid, b.payment_method
       FROM bookings b WHERE b.id=$1 FOR UPDATE`,
      [id]
    );
    if (!bookingRes.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Booking not found' });
    }
    const booking = bookingRes.rows[0];
    // Only allow confirming pending bookings
    if (!['pending'].includes(booking.status)) {
      await rollbackAndRelease(client);
      return res.status(409).json({ message: 'Booking cannot be confirmed', status: booking.status });
    }

    // Determine final status based on payment method
    // For offline/cash payments, keep status as 'pending' to indicate waiting for in-person confirmation
    // For online payments (card, QR), set status to 'confirmed'
    const normalizedMethod = (payment_method || booking.payment_method || 'card').toLowerCase();
    const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);
    const finalStatus = isOfflinePayment ? 'pending' : 'confirmed';

    // For offline payments, don't mark as paid yet; for online payments, record full payment
    const paidAmount = isOfflinePayment ? 0 : (Number(booking.total_amount) || 0);
    const paymentMeta = { method: normalizedMethod, note: isOfflinePayment ? 'Waiting for counter payment' : 'Online payment confirmed' };

    console.log(`[bookings/${id}/payment] Updating: status='${finalStatus}', price_paid=${paidAmount}, payment_method='${normalizedMethod}'`);

    // ✅ Update booking status and payment info
    // Split into two queries to ensure data integrity
    try {
      await client.query(
        `UPDATE bookings SET status=$1, price_paid=$2, payment_method=$3, paid_at=NOW() WHERE id=$4`,
        [finalStatus, paidAmount, normalizedMethod, id]
      );
      console.log(`[bookings/${id}/payment] Main update successful`);
    } catch (updateErr) {
      console.error(`[bookings/${id}/payment] Main update failed:`, updateErr.message);
      throw updateErr;
    }

    // ✅ Update metadata separately
    try {
      await client.query(
        `UPDATE bookings SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb WHERE id=$2`,
        [JSON.stringify({ payment: paymentMeta }), id]
      );
      console.log(`[bookings/${id}/payment] Metadata update successful`);
    } catch (metaErr) {
      console.warn(`[bookings/${id}/payment] Metadata update warning:`, metaErr.message);
      // Don't fail on metadata error
    }

    // ✅ Properly handle COMMIT with error catching
    await client.query('COMMIT');
    client.release();

    console.log(`[bookings/${id}/payment] ✅ Payment confirmed: status='${finalStatus}'`);
    res.json({ message: 'Payment confirmed', booking_id: Number(id), status: finalStatus, paidAmount });
  } catch (err) {
    console.error(`[bookings/${id}/payment] ❌ Error:`, err.message || err);
    console.error(`[bookings/${id}/payment] Stack:`, err.stack);
    try {
      await client.query('ROLLBACK');
    } catch (rollbackErr) {
      console.error(`[bookings/${id}/payment] Rollback error:`, rollbackErr.message);
    }
    client.release();
    res.status(500).json({ message: 'Could not confirm payment' });
  }
});

// PUT/PATCH /bookings/:id/payment-method - Change payment method for a pending booking
router.put('/bookings/:id/payment-method', async (req, res) => {
  const { id } = req.params;
  const { payment_method } = req.body || {};

  if (!payment_method) {
    return res.status(400).json({ message: 'payment_method is required' });
  }

  const client = await beginTransaction();
  try {
    await client.query('BEGIN');

    const bookingRes = await client.query(
      `SELECT b.id, b.trip_id, b.status, b.total_amount, b.metadata, t.departure_time
       FROM bookings b
       JOIN trips t ON t.id = b.trip_id
       WHERE b.id=$1 FOR UPDATE`,
      [id]
    );

    if (!bookingRes.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Booking not found' });
    }

    const booking = bookingRes.rows[0];

    // Check if booking is in a state that allows payment method change
    if (!['pending', 'expired'].includes(booking.status)) {
      await rollbackAndRelease(client);
      return res.status(409).json({
        message: 'Cannot change payment method for this booking status',
        status: booking.status
      });
    }

    // Check if trip has not departed yet
    const now = new Date();
    const departureTime = new Date(booking.departure_time);
    if (departureTime <= now) {
      await rollbackAndRelease(client);
      return res.status(400).json({
        message: 'Cannot change payment method - trip has already departed',
        departure_time: booking.departure_time
      });
    }

    // Normalize payment method
    const normalizedMethod = payment_method.toLowerCase();
    const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);

    // If changing to offline payment and booking was expired, restore it to pending
    let newStatus = booking.status;
    if (booking.status === 'expired' && isOfflinePayment) {
      newStatus = 'pending';
      console.log(`Restoring expired booking ${id} to pending (changed to offline payment)`);
    }

    // Update payment method and status
    const paymentMeta = {
      method: normalizedMethod,
      changed_at: new Date().toISOString(),
      note: isOfflinePayment ? 'Payment at counter' : 'Online payment'
    };

    await client.query(
      `UPDATE bookings
       SET payment_method=$1,
           status=$2,
           metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
       WHERE id=$4`,
      [normalizedMethod, newStatus, JSON.stringify({ payment: paymentMeta }), id]
    );

    await commitAndRelease(client);

    res.json({
      message: 'Payment method updated successfully',
      booking_id: Number(id),
      payment_method: normalizedMethod,
      status: newStatus,
      note: isOfflinePayment ? 'Booking will not expire automatically. Please pay at the counter.' : 'Please complete payment within the time limit.'
    });

  } catch (err) {
    console.error('Update payment method failed:', err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Could not update payment method' });
  }
});

// PATCH alias for payment-method update
router.patch('/bookings/:id/payment-method', async (req, res) => {
  // Reuse PUT handler
  req.method = 'PUT';
  router.handle(req, res);
});

// Force expire a booking (admin/debug)
router.post('/bookings/:id/expire', async (req, res) => {
  const { id } = req.params;
  // TODO: Add admin auth check here
  const client = await beginTransaction();
  try {
    await client.query('BEGIN');
    const bookingRes = await client.query('SELECT b.id, b.trip_id, b.status FROM bookings b WHERE b.id=$1 FOR UPDATE', [id]);
    if (!bookingRes.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Booking not found' });
    }
    const booking = bookingRes.rows[0];
    if (booking.status !== 'pending') {
      await rollbackAndRelease(client);
      return res.status(400).json({ message: 'Only pending bookings can be expired' });
    }

    await client.query("UPDATE bookings SET status='expired', expired_at=NOW() WHERE id=$1", [id]);
    const { rowCount: releasedCount } = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1', [id]);
    if (releasedCount > 0) {
      await client.query('UPDATE trips SET seats_available = seats_available + $1 WHERE id=$2', [releasedCount, booking.trip_id]);
    }
    await commitAndRelease(client);
    res.json({ message: 'Booking manually expired' });
  } catch (err) {
    console.error('Expire booking failed:', err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Failed to expire booking' });
  }
});

// POST /bookings/:id/confirm-offline-payment - Admin confirms offline payment
// Changes booking status from 'pending' (offline) to 'confirmed'
router.post('/bookings/:id/confirm-offline-payment', async (req, res) => {
  const { id } = req.params;
  // TODO: Add admin auth check here
  const client = await beginTransaction();
  try {
    await client.query('BEGIN');
    const bookingRes = await client.query(
      `SELECT b.id, b.trip_id, b.status, b.total_amount, b.payment_method, b.user_id
       FROM bookings b WHERE b.id=$1 FOR UPDATE`,
      [id]
    );
    if (!bookingRes.rowCount) {
      await rollbackAndRelease(client);
      return res.status(404).json({ message: 'Booking not found' });
    }
    const booking = bookingRes.rows[0];

    // Check if this is an offline payment booking in pending status
    const normalizedMethod = (booking.payment_method || '').toLowerCase();
    const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);

    if (!isOfflinePayment) {
      await rollbackAndRelease(client);
      return res.status(400).json({
        message: 'Can only confirm offline payments',
        payment_method: booking.payment_method
      });
    }

    if (booking.status !== 'pending') {
      await rollbackAndRelease(client);
      return res.status(409).json({
        message: 'Booking must be in pending status to confirm offline payment',
        current_status: booking.status
      });
    }

    // Update booking: mark as confirmed and record payment
    const totalAmount = Number(booking.total_amount) || 0;
    const paymentMeta = {
      method: normalizedMethod,
      confirmed_by: 'admin',
      confirmed_at: new Date().toISOString(),
      note: 'Offline payment confirmed by admin'
    };

    await client.query(
      `UPDATE bookings
       SET status=$1, price_paid=$2, paid_at=NOW(), metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
       WHERE id=$4`,
      ['confirmed', totalAmount, JSON.stringify({ payment: paymentMeta }), id]
    );

    await commitAndRelease(client);

    res.json({
      message: 'Offline payment confirmed successfully',
      booking_id: Number(id),
      status: 'confirmed',
      price_paid: totalAmount
    });
  } catch (err) {
    console.error('Confirm offline payment failed:', err.message || err);
    await rollbackAndRelease(client);
    res.status(500).json({ message: 'Failed to confirm offline payment' });
  }
});

module.exports = router;

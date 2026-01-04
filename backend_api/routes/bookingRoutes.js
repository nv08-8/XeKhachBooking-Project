const express = require("express");
const router = express.Router();
const db = require("../db");
const { generateAndCacheSeats } = require("../utils/seatGenerator");
const cron = require("node-cron");
const sendPaymentConfirmationEmail = require("../utils/sendPaymentEmail");
const SUPPORT_CONFIG = require("../config/supportConfig");

// Will be set by server.js after io is initialized
let io = null;

/**
 * Generate a unique booking code for ticket verification
 * Format: XK{TRIP_ID}{RANDOM_HASH} (e.g., XK99A7B2C5)
 * Can be encoded into QR code for offline verification
 */
function generateBookingCode(tripId) {
  // Generate a random hex string (6 characters)
  const randomPart = Math.random().toString(16).substring(2, 8).toUpperCase();
  // Pad trip_id with zeros to ensure it's 4 digits
  const tripPart = String(tripId).padStart(4, '0');
  return `XK${tripPart}${randomPart}`;
}

/**
 * Set Socket.IO instance for real-time notifications
 */
function setSocketIO(socketIO) {
  io = socketIO;
  console.log("✅ Socket.IO connected to booking routes");
}

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

// Helper to generate ISO-like timestamp without trailing Z (UTC-based, no timezone shift)
function formatLocalISO(date = new Date()) {
  // Return the UTC ISO string but without the trailing 'Z' so we don't force UTC interpretation on clients.
  // This preserves the hour as stored (e.g., '2026-01-01T09:00:00.000Z' -> '2026-01-01T09:00:00.000').
  try {
    return (date instanceof Date ? date.toISOString() : new Date(date).toISOString()).replace(/Z$/, '');
  } catch (e) {
    return String(date);
  }
}

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
      `SELECT t.price, t.seats_available, t.departure_time, t.arrival_time, ((t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') <= NOW()) AS has_departed
       FROM trips t WHERE t.id=$1 FOR UPDATE`,
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
    if (trip.has_departed) {
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

    // Generate unique booking code for QR verification
    const bookingCode = generateBookingCode(trip_id);

    const bookingInsert = await client.query(
      `INSERT INTO bookings (user_id, trip_id, total_amount, seats_count, promotion_code, status, metadata, pickup_stop_id, dropoff_stop_id, payment_method, passenger_info, booking_code)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) RETURNING id`,
      [user_id, trip_id, finalAmount, requiredSeats, promotion_code || null, 'pending', metadata ? JSON.stringify(metadata) : null, pickup_stop_id, dropoff_stop_id, finalPaymentMethod, passengerInfoJson, bookingCode]
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

    console.log(`✅ [BOOKING-CREATED] Booking #${bookingId} created: user=${user_id}, trip=${trip_id}, payment_method="${finalPaymentMethod}", status="pending", seats=[${seat_labels.join(',')}]`);

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
    // Tab "Đã đi": Only completed trips (departure_time passed and status = 'completed')
    // Excludes cancelled bookings
    // Interpret stored departure_time as Asia/Ho_Chi_Minh (local VN time)
    whereClause += ` AND (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW() AND b.status = 'completed'`;
  }
  // If no tab specified, return all bookings (backwards compatibility)

  const sql = `
    SELECT b.id, b.status, b.price_paid, b.created_at, b.total_amount, b.payment_method,
           b.passenger_info, COALESCE(b.booking_code, '') as booking_code,
           t.id AS trip_id, t.departure_time, t.arrival_time, t.operator, t.bus_type, t.status AS trip_status,
           r.origin, r.destination,
           pickup_stop.name AS pickup_location,
           dropoff_stop.name AS dropoff_location,
           COALESCE(
             array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL),
             ARRAY[]::text[]
           ) AS seat_labels
    FROM bookings b
    LEFT JOIN trips t ON t.id = b.trip_id
    LEFT JOIN routes r ON r.id = t.route_id
    LEFT JOIN booking_items bi ON bi.booking_id = b.id
    LEFT JOIN route_stops pickup_stop ON pickup_stop.id = b.pickup_stop_id
    LEFT JOIN route_stops dropoff_stop ON dropoff_stop.id = b.dropoff_stop_id
    ${whereClause}
    GROUP BY b.id, t.id, r.id, pickup_stop.id, dropoff_stop.id
    ORDER BY COALESCE(t.departure_time, b.created_at) DESC
  `;
  try {
    const { rows } = await db.query(sql, [user_id]);
    console.log(`   ✅ Found ${rows.length} bookings for user ${user_id} (tab: ${tab || 'all'})`);
    if (rows.length > 0) {
      console.log(`   First booking: #${rows[0].id}, status: ${rows[0].status}, route: ${rows[0].origin} → ${rows[0].destination}`);
    }
    // Format departure_time and arrival_time to local ISO without trailing Z
    const formattedRows = rows.map(r => ({
      ...r,
      departure_time: r.departure_time ? formatLocalISO(new Date(r.departure_time)) : r.departure_time,
      arrival_time: r.arrival_time ? formatLocalISO(new Date(r.arrival_time)) : r.arrival_time,
      created_at: r.created_at ? formatLocalISO(new Date(r.created_at)) : r.created_at,
      // Thêm thông báo nếu trip bị hủy hoặc admin hủy vé hoặc user hủy vé đã thanh toán
      trip_cancelled_message:
        (r.trip_status === 'cancelled' || r.status === 'pending_refund' || (r.status === 'cancelled' && r.price_paid > 0))
          ? SUPPORT_CONFIG.TRIP_CANCELLED_MESSAGE(SUPPORT_CONFIG.REFUND_HOTLINE)
          : null,
      support_hotline: (r.trip_status === 'cancelled' || r.status === 'pending_refund' || (r.status === 'cancelled' && r.price_paid > 0)) ? SUPPORT_CONFIG.REFUND_HOTLINE : null
    }));
    res.json(formattedRows);
  } catch (err) {
    console.error('❌ Failed to fetch bookings:', err);
    res.status(500).json({ message: 'Failed to fetch bookings' });
  }
});

// GET /bookings/:id - Get specific booking details
router.get('/bookings/:id', async (req, res) => {
  const { id } = req.params;
  const sql = `
    SELECT b.id, b.user_id, b.trip_id, b.total_amount, b.seats_count, b.promotion_code,
           b.status, b.metadata, b.pickup_stop_id, b.dropoff_stop_id, b.payment_method,
           b.passenger_info, b.created_at, b.paid_at, b.cancelled_at, b.expired_at,
           b.price_paid, COALESCE(b.booking_code, '') as booking_code,
           t.departure_time, t.arrival_time, t.operator, t.bus_type, t.price AS seat_price, t.status AS trip_status,
           r.origin, r.destination,
           u.name AS passenger_name, u.phone AS passenger_phone,
           pickup_stop.name AS pickup_location, pickup_stop.address AS pickup_address,
           dropoff_stop.name AS dropoff_location, dropoff_stop.address AS dropoff_address,
           COALESCE(
             array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL),
             ARRAY[]::text[]
           ) AS seat_labels
    FROM bookings b
    LEFT JOIN trips t ON t.id = b.trip_id
    LEFT JOIN routes r ON r.id = t.route_id
    LEFT JOIN users u ON u.id = b.user_id
    LEFT JOIN booking_items bi ON bi.booking_id = b.id
    LEFT JOIN route_stops pickup_stop ON pickup_stop.id = b.pickup_stop_id
    LEFT JOIN route_stops dropoff_stop ON dropoff_stop.id = b.dropoff_stop_id
    WHERE b.id=$1
    GROUP BY b.id, t.id, t.departure_time, t.arrival_time, t.operator, t.bus_type, t.price, t.status,
             r.id, r.origin, r.destination,
             u.id, u.name, u.phone,
             pickup_stop.id, pickup_stop.name, pickup_stop.address,
             dropoff_stop.id, dropoff_stop.name, dropoff_stop.address
  `;
  try {
    const { rows } = await db.query(sql, [id]);
    if (!rows.length) return res.status(404).json({ message: 'Booking not found' });

    const booking = rows[0];

    // Calculate base_price and discount_amount
    const seatsCount = booking.seats_count || 0;
    const seatPrice = parseFloat(booking.seat_price) || 0;
    const basePrice = seatPrice * seatsCount;

    // Use total_amount as the source of truth (it's the price after all discounts including coins during checkout)
    const totalAmount = parseFloat(booking.total_amount) || 0;

    // Fetch coin discount from coin_history table
    // This is the actual amount of coins used (stored as negative in coin_history)
    let coinDiscount = 0;
    let promoDiscount = 0;

    try {
      const coinHistRes = await db.query(
        `SELECT ABS(SUM(amount)) as total_coins FROM coin_history WHERE booking_id=$1 AND type='spend'`,
        [id]
      );
      if (coinHistRes.rows && coinHistRes.rows[0] && coinHistRes.rows[0].total_coins) {
        coinDiscount = parseFloat(coinHistRes.rows[0].total_coins) || 0;
      }
    } catch (e) {
      console.error('Failed to fetch coin discount from coin_history:', e);
      coinDiscount = 0;
    }

    // Calculate promotion discount
    // promoDiscount = basePrice - coinDiscount - totalAmount
    if (basePrice > totalAmount) {
      const totalDiscount = basePrice - totalAmount;
      promoDiscount = Math.max(0, totalDiscount - coinDiscount);
    } else {
      promoDiscount = 0;
    }

    // Add calculated fields to response
    booking.base_price = basePrice;
    booking.discount_amount = promoDiscount; // Promotion discount only
    booking.coin_discount = coinDiscount; // Coin discount
    booking.promo_code = booking.promotion_code; // Alias for consistency

    // Format trip times and created_at/published times to local ISO without trailing Z
    if (booking.departure_time) booking.departure_time = formatLocalISO(new Date(booking.departure_time));
    if (booking.arrival_time) booking.arrival_time = formatLocalISO(new Date(booking.arrival_time));
    if (booking.created_at) booking.created_at = formatLocalISO(new Date(booking.created_at));
    if (booking.paid_at) booking.paid_at = formatLocalISO(new Date(booking.paid_at));

    // Thêm thông báo nếu trip bị hủy hoặc admin hủy vé
    if (booking.trip_status === 'cancelled' || booking.status === 'pending_refund') {
      booking.trip_cancelled_message = SUPPORT_CONFIG.TRIP_CANCELLED_MESSAGE(SUPPORT_CONFIG.REFUND_HOTLINE);
      booking.support_hotline = SUPPORT_CONFIG.REFUND_HOTLINE;
    }

    res.json(booking);
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

    // Compute hours until departure using DB to avoid timezone issues
    const deltaRes = await client.query("SELECT EXTRACT(EPOCH FROM ((t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') - NOW()))/3600.0 AS hours_until FROM trips t WHERE t.id=$1", [booking.trip_id]);
    const hoursUntilDeparture = deltaRes && deltaRes.rows && deltaRes.rows[0] ? Number(deltaRes.rows[0].hours_until) : null;
    if (hoursUntilDeparture === null || hoursUntilDeparture <= 2) {
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

  // Helper: simple Luhn implementation
  function luhnCheck(number) {
    if (!number) return false;
    const s = String(number).replace(/\D/g, '');
    if (s.length < 12 || s.length > 19) return false;
    let sum = 0;
    let alt = false;
    for (let i = s.length - 1; i >= 0; i--) {
      let n = parseInt(s.charAt(i), 10);
      if (alt) {
        n *= 2;
        if (n > 9) n = (n % 10) + 1;
      }
      sum += n;
      alt = !alt;
    }
    return (sum % 10) === 0;
  }

  // Helper: expiry validator MM/YY or MM/YYYY
  function expiryValid(expiry) {
    if (!expiry) return false;
    expiry = String(expiry).trim();
    let parts = null;
    if (expiry.indexOf('/') !== -1) parts = expiry.split('/');
    else if (expiry.indexOf('-') !== -1) parts = expiry.split('-');
    else if (expiry.length === 4) parts = [expiry.substring(0,2), expiry.substring(2)];
    if (!parts || parts.length < 2) return false;
    const month = parseInt(parts[0], 10);
    let year = parseInt(parts[1], 10);
    if (isNaN(month) || isNaN(year)) return false;
    if (month < 1 || month > 12) return false;
    if (year < 100) year += 2000;
    // end of month
    const now = new Date();
    const exp = new Date(year, month, 0, 23, 59, 59, 999); // last ms of month
    return exp.getTime() > now.getTime();
  }

  // Simulated card gateway
  async function simulateCardCharge(amount, cardNumber) {
    // simple simulation: decline if last4 == '0000'
    const s = String(cardNumber).replace(/\D/g, '');
    const last4 = s.slice(-4);
    if (last4 === '0000') return { success: false, reason: 'card_declined' };
    // success: return transaction id and brand
    const brand = s.charAt(0) === '4' ? 'Visa' : (s.charAt(0) === '5' ? 'MasterCard' : 'Card');
    return { success: true, transaction_id: 'sim_txn_' + Date.now(), brand, last4 };
  }

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

    // If payment method is card, validate card fields from body
    const normalizedMethod = (payment_method || booking.payment_method || 'card').toLowerCase();
    const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);

    if (normalizedMethod === 'card') {
      const { card_number, expiry, cvv } = req.body || {};
      // Validate presence
      if (!card_number || !expiry || !cvv) {
        await rollbackAndRelease(client);
        return res.status(400).json({ message: 'Missing card information' });
      }
      // Server-side validation
      if (!luhnCheck(card_number)) {
        await rollbackAndRelease(client);
        return res.status(400).json({ message: 'Invalid card number' });
      }
      if (!expiryValid(expiry)) {
        await rollbackAndRelease(client);
        return res.status(400).json({ message: 'Card expired or invalid expiry' });
      }
      if (!/^[0-9]{3,4}$/.test(String(cvv))) {
        await rollbackAndRelease(client);
        return res.status(400).json({ message: 'Invalid CVV' });
      }

      // Simulate charging the card (dev-only). Use booking.total_amount as amount
      const amount = Number(booking.total_amount) || 0;
      const chargeResult = await simulateCardCharge(amount, card_number);
      if (!chargeResult.success) {
        await rollbackAndRelease(client);
        return res.status(402).json({ error: 'payment_failed', reason: chargeResult.reason });
      }

      // On success: update booking as confirmed and set price_paid, paid_at
      const finalStatus = 'confirmed';
      const paidAmount = Number(booking.total_amount) || 0;

      try {
        await client.query(
          `UPDATE bookings SET status=$1, price_paid=$2, payment_method=$3, paid_at=NOW() WHERE id=$4`,
          [finalStatus, paidAmount, normalizedMethod, id]
        );
      } catch (updateErr) {
        console.error(`[bookings/${id}/payment] Main update failed:`, updateErr.message);
        throw updateErr;
      }

      // Add safe metadata: last4, brand, gateway id (do not store full PAN or CVV)
      const safeMeta = { card: { brand: chargeResult.brand, last4: chargeResult.last4, gateway_transaction_id: chargeResult.transaction_id } };
      try {
        await client.query(
          `UPDATE bookings SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb WHERE id=$2`,
          [JSON.stringify({ payment: safeMeta }), id]
        );
      } catch (metaErr) {
        console.warn(`[bookings/${id}/payment] Metadata update warning:`, metaErr.message);
      }

      await client.query('COMMIT');
      client.release();
      console.log(`[bookings/${id}/payment] ✅ Card payment simulated and booking confirmed`);
      return res.json({ message: 'Payment confirmed', booking_id: Number(id), status: finalStatus, paidAmount });
    }

    // Non-card flow: existing behavior
    const finalStatus = isOfflinePayment ? 'pending' : 'confirmed';
    const paidAmount = isOfflinePayment ? 0 : (Number(booking.total_amount) || 0);
    const paymentMeta = { method: normalizedMethod, note: isOfflinePayment ? 'Waiting for counter payment' : 'Online payment confirmed' };

    console.log(`[bookings/${id}/payment] Updating: status='${finalStatus}', price_paid=${paidAmount}, payment_method='${normalizedMethod}'`);

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

    try {
      await client.query(
        `UPDATE bookings SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb WHERE id=$2`,
        [JSON.stringify({ payment: paymentMeta }), id]
      );
      console.log(`[bookings/${id}/payment] Metadata update successful`);
    } catch (metaErr) {
      console.warn(`[bookings/${id}/payment] Metadata update warning:`, metaErr.message);
    }

    await client.query('COMMIT');
    client.release();

    console.log(`[bookings/${id}/payment] ✅ Payment confirmed: status='${finalStatus}'`);

    // Send confirmation email for online payments only
    if (!isOfflinePayment) {
      try {
        console.log(`📧 Sending email for online payment: booking ${id}`);
        const fullBooking = await db.query(
          `SELECT b.*, u.email, u.name, u.phone, r.origin, r.destination, t.departure_time, t.operator, t.bus_type,
                  COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) AS seat_labels
           FROM bookings b
           JOIN users u ON b.user_id = u.id
           JOIN trips t ON b.trip_id = t.id
           JOIN routes r ON t.route_id = r.id
           LEFT JOIN booking_items bi ON bi.booking_id = b.id
           WHERE b.id=$1
           GROUP BY b.id, b.booking_code, b.trip_id, b.total_amount, b.price_paid, b.payment_method, b.status, b.created_at, b.paid_at, b.user_id, u.id, u.email, u.name, u.phone, r.id, r.origin, r.destination, t.id, t.departure_time, t.operator, t.bus_type`,
          [id]
        );

        if (fullBooking.rowCount > 0) {
          const booking = fullBooking.rows[0];
          const userData = {
            email: booking.email,
            name: booking.name,
            phone: booking.phone
          };
          const tripData = {
            origin: booking.origin,
            destination: booking.destination,
            departure_time: booking.departure_time,
            operator: booking.operator,
            bus_type: booking.bus_type
          };
          await sendPaymentConfirmationEmail(booking.email, booking, tripData, userData);
          console.log(`✅ Email sent for booking ${id}`);
        }
      } catch (emailError) {
        console.error(`❌ Failed to send email for booking ${id}:`, emailError.message || emailError);
        // Don't fail the payment confirmation if email fails
      }
    }

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

// POST /bookings/:id/verify-payment - quick status check for client polling
router.post('/bookings/:id/verify-payment', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await db.query(
      `SELECT id, status, total_amount, price_paid, payment_method, COALESCE(booking_code, '') as booking_code
       FROM bookings WHERE id = $1 LIMIT 1`,
      [id]
    );
    if (!result.rows || result.rows.length === 0) {
      return res.status(404).json({ message: 'Booking not found' });
    }
    const b = result.rows[0];
    // Return minimal, stable payload for client polling
    return res.json({
      id: Number(b.id),
      status: b.status,
      total_amount: Number(b.total_amount) || 0,
      price_paid: Number(b.price_paid) || 0,
      payment_method: b.payment_method || null,
      booking_code: b.booking_code || ''
    });
  } catch (err) {
    console.error(`[verify-payment] Error checking booking ${id}:`, err);
    return res.status(500).json({ message: 'Failed to verify payment' });
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
    if (!['pending'].includes(booking.status)) {
      await rollbackAndRelease(client);
      return res.status(409).json({
        message: 'Cannot change payment method for this booking status',
        status: booking.status
      });
    }

    // Check if trip has not departed yet using DB time
    const hasDepRes = await client.query("SELECT ((departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') <= NOW()) AS has_departed FROM trips WHERE id=$1", [booking.trip_id]);
    const hasDeparted = hasDepRes && hasDepRes.rows && hasDepRes.rows[0] ? (hasDepRes.rows[0].has_departed === true || hasDepRes.rows[0].has_departed === 't') : false;
    if (hasDeparted) {
      await rollbackAndRelease(client);
      return res.status(400).json({
        message: 'Cannot change payment method - trip has already departed',
        departure_time: booking.departure_time
      });
    }

    // Normalize payment method
    const normalizedMethod = payment_method.toLowerCase();
    const isOfflinePayment = ['cash', 'offline', 'cod', 'counter'].includes(normalizedMethod);

    // If changing payment method for a pending booking, status may be updated
    let newStatus = booking.status;

    // Update payment method and status
    const paymentMeta = {
      method: normalizedMethod,
      changed_at: formatLocalISO(),
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
      return res.status(400).json({ message: 'Only pending bookings can be cancelled via this endpoint' });
    }

    // Cancel the pending booking immediately (do not use 'expired' status)
    await client.query("UPDATE bookings SET status='cancelled', cancelled_at=NOW() WHERE id=$1", [id]);
    const { rowCount: releasedCount } = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1', [id]);
    if (releasedCount > 0) {
      await client.query('UPDATE trips SET seats_available = seats_available + $1 WHERE id=$2', [releasedCount, booking.trip_id]);
    }
    await commitAndRelease(client);
    res.json({ message: 'Booking manually cancelled' });
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
      confirmed_at: formatLocalISO(),
      note: 'Offline payment confirmed by admin'
    };

    await client.query(
      `UPDATE bookings
       SET status=$1, price_paid=$2, paid_at=NOW(), metadata = COALESCE(metadata, '{}'::jsonb) || $3::jsonb
       WHERE id=$4`,
      ['confirmed', totalAmount, JSON.stringify({ payment: paymentMeta }), id]
    );

    await commitAndRelease(client);

    // Send confirmation email for offline payment after commit
    try {
      console.log(`📧 Sending email for offline payment confirmation: booking ${id}`);
      const fullBooking = await db.query(
        `SELECT b.*, u.email, u.name, u.phone, r.origin, r.destination, t.departure_time, t.operator, t.bus_type
         FROM bookings b
         JOIN users u ON b.user_id = u.id
         JOIN trips t ON b.trip_id = t.id
         JOIN routes r ON t.route_id = r.id
         WHERE b.id=$1`,
        [id]
      );

      if (fullBooking.rowCount > 0) {
        const bookingData = fullBooking.rows[0];
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
        await sendPaymentConfirmationEmail(bookingData.email, bookingData, tripData, userData);
        console.log(`✅ Email sent for offline payment confirmation: booking ${id}`);
      }
    } catch (emailError) {
      console.error(`❌ Failed to send email for booking ${id}:`, emailError.message || emailError);
      // Don't fail the payment confirmation if email fails
    }

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

// ==========================================
// CRON JOB: Auto-finalize bookings
// ==========================================

/**
 * Cron Job: Auto-finalize bookings after trip arrival
 * Runs every 5 minutes to:
 * 1. Complete paid bookings (confirmed + price_paid > 0)
 * 2. Cancel unpaid bookings (pending + price_paid = 0)
 */
cron.schedule("*/5 * * * *", async () => {
  try {
    console.log("\n🕐 [CRON] Finalizing bookings after trip arrival...");

    // 1️⃣ Complete paid bookings
    const completeResult = await db.query(`
      UPDATE bookings b
      SET status = 'completed', completed_at = NOW()
      FROM trips t
      WHERE b.trip_id = t.id
        AND (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()
        AND (
          b.status = 'confirmed'
          OR b.paid_at IS NOT NULL
          OR COALESCE(b.price_paid, 0) > 0
        )
        AND b.completed_at IS NULL
      RETURNING b.id, b.user_id, b.trip_id, t.departure_time
    `);

    const completedBookings = completeResult.rows;

    // 2️⃣ Cancel unpaid bookings after trip arrival (both online and offline)
    const cancelResult = await db.query(`
      UPDATE bookings b
      SET status = 'cancelled', cancelled_at = NOW()
      FROM trips t
      WHERE b.trip_id = t.id
        AND (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()
        AND b.status = 'pending'
        AND COALESCE(b.price_paid, 0) = 0
        AND b.paid_at IS NULL
        AND b.cancelled_at IS NULL
      RETURNING b.id, b.user_id, b.trip_id, t.departure_time, b.payment_method
    `);

    const cancelledBookings = cancelResult.rows;

    // Log results
    if (completedBookings.length === 0 && cancelledBookings.length === 0) {
      console.log("   ℹ️  No bookings to finalize");
      return;
    }

    // Handle completed bookings
    if (completedBookings.length > 0) {
      console.log(`   ✅ Completed ${completedBookings.length} paid booking(s):`);
      for (const booking of completedBookings) {
        console.log(`      • Booking #${booking.id} -> completed (departure: ${new Date(booking.departure_time).toLocaleString()})`);

        // Emit socket event to user for real-time update
        if (io) {
          io.to(`user_${booking.user_id}`).emit('booking_event', {
            id: booking.id,
            trip_id: booking.trip_id,
            status: 'completed'
          });
        }
      }
    }

    // Handle cancelled bookings
    if (cancelledBookings.length > 0) {
      console.log(`   ❌ Cancelled ${cancelledBookings.length} unpaid booking(s) after trip ended:`);
      for (const booking of cancelledBookings) {
        const method = booking.payment_method || 'unknown';
        const departure = new Date(booking.departure_time).toLocaleString();
        console.log(`      • Booking #${booking.id} (payment: ${method}) -> cancelled (trip departure: ${departure})`);

        // Emit socket event to user for real-time update
        if (io) {
          io.to(`user_${booking.user_id}`).emit('booking_event', {
            id: booking.id,
            trip_id: booking.trip_id,
            status: 'cancelled'
          });
        }
      }
    }

    console.log(`   📊 Summary: ${completedBookings.length} completed, ${cancelledBookings.length} cancelled`);

  } catch (err) {
    console.error("❌ [CRON ERROR] Auto-finalize job failed:", err.message);
  }
});

console.log("✅ Cron job initialized: Auto-finalize bookings every 5 minutes (complete paid, cancel unpaid)");

// POST /bookings/:id/send-confirmation-email - Send payment and booking confirmation email
router.post('/bookings/:id/send-confirmation-email', async (req, res) => {
  const { id } = req.params;

  try {
    // Get booking details
    const bookingRes = await db.query(
      `SELECT b.id, b.booking_code, b.trip_id, b.total_amount,
              b.price_paid, b.payment_method, b.status, b.created_at, b.paid_at, b.user_id,
              COALESCE(array_agg(bi.seat_code) FILTER (WHERE bi.seat_code IS NOT NULL), ARRAY[]::text[]) AS seat_labels
       FROM bookings b
       LEFT JOIN booking_items bi ON bi.booking_id = b.id
       WHERE b.id=$1
       GROUP BY b.id, b.booking_code, b.trip_id, b.total_amount, b.price_paid, b.payment_method, b.status, b.created_at, b.paid_at, b.user_id`,
      [id]
    );

    if (!bookingRes.rowCount) {
      return res.status(404).json({ message: 'Booking not found' });
    }

    const booking = bookingRes.rows[0];

    // Get trip details
    const tripRes = await db.query(
      `SELECT t.id, t.departure_time, t.operator, t.bus_type, r.origin, r.destination
       FROM trips t
       JOIN routes r ON t.route_id = r.id
       WHERE t.id=$1`,
      [booking.trip_id]
    );

    if (!tripRes.rowCount) {
      return res.status(404).json({ message: 'Trip not found' });
    }

    const trip = tripRes.rows[0];

    // Get user details (customer)
    const userRes = await db.query(
      `SELECT id, name, email, phone FROM users WHERE id=$1`,
      [booking.user_id]
    );

    if (!userRes.rowCount) {
      return res.status(404).json({ message: 'User not found' });
    }

    const user = userRes.rows[0];

    // Import the send email function
    const sendPaymentConfirmationEmail = require('../utils/sendPaymentEmail');

    // Send email
    const emailSent = await sendPaymentConfirmationEmail(user.email, booking, trip, user);

    if (emailSent) {
      res.json({
        success: true,
        message: 'Payment confirmation email sent successfully',
        email: user.email
      });
    } else {
      res.status(500).json({
        success: false,
        message: 'Failed to send email, but booking is confirmed'
      });
    }
  } catch (err) {
    console.error('Error sending confirmation email:', err.message || err);
    res.status(500).json({
      success: false,
      message: 'Failed to send confirmation email'
    });
  }
});

module.exports = router;
module.exports.setSocketIO = setSocketIO;

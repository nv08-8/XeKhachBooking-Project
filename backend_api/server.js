require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const db = require("./db");
const http = require('http');
const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const path = require('path');
const fs = require('fs');
const { runMigrations } = require("./migrations/run_migrations");

const authRoutes = require("./routes/authRoutes");
const tripRoutes = require("./routes/tripRoutes");
const dataRoutes = require("./routes/dataRoutes");
const bookingRoutes = require("./routes/bookingRoutes");
const promoRoutes = require("./routes/promoRoutes");
const metaRoutes = require("./routes/metaRoutes");
const paymentRoutes = require("./routes/paymentRoutes");
const busImageRoutes = require("./routes/busImageRoutes");
const seatsRoutes = require("./routes/seatsRoutes");
const adminRoutes = require("./routes/adminRoutes");
const driversRoutes = require("./routes/driversRoutes"); 
const feedbackRoutes = require("./routes/feedbackRoutes");
const coinRoutes = require("./routes/coinRoutes"); // Import coin routes


app.use(express.json());
app.use(cors());

// Create uploads directory if it doesn't exist
const uploadDir = path.join(__dirname, 'uploads', 'avatars');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Serve static files from uploads directory
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Middleware UTF-8
app.use((req, res, next) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    next();
});

app.use("/api/auth", authRoutes);
app.use("/api", tripRoutes);
app.use("/api", dataRoutes);
app.use("/api", bookingRoutes);
app.use("/api", promoRoutes);
app.use("/api", metaRoutes);
app.use("/api/payment", paymentRoutes);
app.use("/api", busImageRoutes);
app.use('/api/seats', seatsRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/admin/drivers", driversRoutes);
app.use("/api", feedbackRoutes);
app.use("/api", coinRoutes); // Use coin routes

// small helper to list registered routes (useful for debugging 404s/method mismatches)
function listRoutes(app) {
    try {
        const routes = [];
        app._router.stack.forEach((middleware) => {
            if (middleware.route) {
                // routes registered directly on the app
                const methods = Object.keys(middleware.route.methods).map(m => m.toUpperCase()).join(',');
                routes.push(`${methods} ${middleware.route.path}`);
            } else if (middleware.name === 'router') {
                // router middleware
                middleware.handle.stack.forEach(function(handler) {
                    if (handler.route) {
                        const methods = Object.keys(handler.route.methods).map(m => m.toUpperCase()).join(',');
                        routes.push(`${methods} ${handler.route.path}`);
                    }
                });
            }
        });
        console.log('Registered routes:\n' + routes.join('\n'));
    } catch (e) {
        console.warn('Could not list routes:', e.message || e);
    }
}

app.get("/", (req, res) => {
    res.send("XeKhachBooking API chạy bằng PostgreSQL trên Render nè!");
});
app.get("/api/config/maps-key", (req, res) => {
    res.json({
        maps_api_key: process.env.GOOGLE_MAPS_API_KEY || null
    });
});

// Serve logo for email (from assets folder)
app.get("/api/config/logo", (req, res) => {
    const logoPath = require('path').join(__dirname, 'assets', 'logo.jpg');
    res.sendFile(logoPath, (err) => {
        if (err) {
            // If logo not found, return a placeholder image or error response
            res.status(404).json({ error: "Logo not found" });
        }
    });
});

// Initialize socket.io server
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*', // change to your client origin in production
    methods: ['GET', 'POST']
  }
});

const JWT_SECRET = process.env.JWT_SECRET || 'dev_jwt_secret_change_me';

// Connect Socket.IO to booking routes for CRON job real-time updates
bookingRoutes.setSocketIO(io);

// Simple socket auth: client passes ?token=JWT; server verifies token and lets socket join user room
io.on('connection', async (socket) => {
  try {
    const token = socket.handshake.query && socket.handshake.query.token;
    if (!token) {
      socket.disconnect(true);
      return;
    }

    let payload;
    try {
      payload = jwt.verify(token, JWT_SECRET);
    } catch (err) {
      console.warn('Socket auth token verify failed', err.message || err);
      socket.disconnect(true);
      return;
    }

    const userId = payload && payload.id;
    if (!userId) {
      socket.disconnect(true);
      return;
    }

    // verify user exists
    const { rows } = await db.query('SELECT id FROM users WHERE id=$1 AND status=$2', [userId, 'active']);
    if (!rows || rows.length === 0) {
      socket.disconnect(true);
      return;
    }

    const room = `user_${userId}`;
    socket.join(room);
    console.log(`Socket ${socket.id} joined room ${room}`);

    socket.on('disconnect', () => {
      console.log('Socket disconnected', socket.id);
    });
  } catch (err) {
    console.warn('Socket auth error', err.message || err);
    socket.disconnect(true);
  }
});

// Background job: expire pending bookings older than configured TTL (default 10 minutes)
// You can override this by setting BOOKING_PENDING_TTL_MINUTES in your environment (e.g., .env).
const BOOKING_PENDING_TTL_MINUTES = parseInt(process.env.BOOKING_PENDING_TTL_MINUTES || '10', 10);
const EXPIRE_CHECK_INTERVAL_MS = 5 * 1000; // run every 5 seconds

async function expirePendingBookings() {
    try {
        // Find pending bookings that MIGHT need expiring
        // Criteria:
        // 1. Online Payment (QR/Card) -> expire after TTL (10 mins)
        // 2. Offline Payment -> NOT EXPIRED AUTOMATICALLY (as per request)
        // ✅ Filter out known offline payment methods at SQL level to avoid fetching them
        // ✅ Also exclude bookings with NULL payment_method (treat as offline for safety)
        const sql = `SELECT b.id, b.user_id, b.trip_id, t.arrival_time, b.metadata, b.created_at, b.payment_method
                     FROM bookings b
                     JOIN trips t ON t.id = b.trip_id
                     WHERE b.status='pending' 
                     AND (b.created_at < NOW() - INTERVAL '${BOOKING_PENDING_TTL_MINUTES} minutes')
                     AND b.payment_method IS NOT NULL
                     AND LOWER(b.payment_method) NOT IN ('cash', 'offline', 'cod', 'counter')
                     AND LOWER(b.payment_method) NOT LIKE '%offline%'
                     AND LOWER(b.payment_method) NOT LIKE '%cash%'`;

        const { rows } = await db.query(sql);
        if (!rows || rows.length === 0) return;

        for (const b of rows) {
            const bookingId = b.id;
            const client = await db.connect();
            try {
                await client.query('BEGIN');

                // Lock and read the full booking row
                const bRes = await client.query('SELECT * FROM bookings WHERE id=$1 FOR UPDATE', [bookingId]);
                if (!bRes.rowCount) {
                    await client.query('ROLLBACK');
                    client.release();
                    continue;
                }
                const booking = bRes.rows[0];
                if (booking.status !== 'pending') {
                    await client.query('ROLLBACK');
                    client.release();
                    continue;
                }

                // ✅ CRITICAL: Check payment_method FIRST - Offline payments should NEVER be auto-expired
                // (Note: SQL filter should exclude most offline bookings; this is a safety fallback)
                if (booking.payment_method) {
                    const method = String(booking.payment_method).toLowerCase();
                    if (['cash', 'offline', 'cod', 'counter'].includes(method)) {
                        // This is an offline payment - DO NOT EXPIRE IT
                        await client.query('ROLLBACK');
                        client.release();
                        console.log(`✅ [EXPIRE-SKIP] Offline payment booking #${booking.id} (method=${method}) - NOT expired`);
                        continue;
                    }
                }

                // Determine if this booking should be cancelled
                // Logic:
                // - Is Online Payment? (Has orderCode in metadata OR explicit payment_method)
                // - If Online: Cancel if created_at > TTL
                // - If Offline: Do NOT cancel

                let isOnlinePayment = false;

                // Check payment_method field first (most reliable)
                if (booking.payment_method) {
                    const method = String(booking.payment_method).toLowerCase();
                    // Offline methods: cash, offline, cod, counter
                    const offlineMethods = ['cash', 'offline', 'cod', 'counter'];
                    const onlineMethods = ['card', 'qr', 'payos', 'credit_card', 'momo', 'vnpay'];

                    if (offlineMethods.includes(method)) {
                        isOnlinePayment = false;
                    } else if (onlineMethods.includes(method)) {
                        isOnlinePayment = true;
                    }
                }

                // Fallback to metadata checks if payment_method is not set or unclear
                if (!booking.payment_method || (!isOnlinePayment && !['cash', 'offline', 'cod', 'counter'].includes(String(booking.payment_method).toLowerCase()))) {
                    if (booking.metadata && booking.metadata.payment) {
                        if (booking.metadata.payment.orderCode) {
                            isOnlinePayment = true;
                        } else if (booking.metadata.payment.method) {
                            const metaMethod = String(booking.metadata.payment.method).toLowerCase();
                            if (['card', 'qr', 'payos', 'credit_card', 'momo', 'vnpay'].includes(metaMethod)) {
                                isOnlinePayment = true;
                            } else if (['cash', 'offline', 'cod', 'counter'].includes(metaMethod)) {
                                isOnlinePayment = false;
                            }
                        }
                    }
                }

                const now = new Date();
                const createdAt = new Date(booking.created_at);
                const ttlLimit = new Date(createdAt.getTime() + BOOKING_PENDING_TTL_MINUTES * 60000);
                
                let shouldCancel = false;
                let newStatus = 'cancelled'; // Default

                console.log(`📋 [EXPIRE-CHECK] Booking #${booking.id}: payment_method="${booking.payment_method}", isOnline=${isOnlinePayment}, age=${Math.round((now - createdAt)/60000)}min`);

                if (isOnlinePayment) {
                    // Online payment: expire if TTL passed
                    if (now > ttlLimit) {
                        shouldCancel = true;
                        newStatus = 'cancelled'; // directly cancel (we no longer use 'expired' status)
                        console.log(`❌ [EXPIRE-YES] Booking #${booking.id} will be cancelled (online payment timeout)`);
                    }
                } else {
                    // Offline/Cash: Do NOT expire automatically
                    shouldCancel = false;
                    console.log(`✅ [EXPIRE-NO] Booking #${booking.id} will NOT be expired (offline payment)`);
                }

                if (!shouldCancel) {
                    await client.query('ROLLBACK');
                    client.release();
                    continue;
                }

                // Proceed to cancel/expire
                // Preferred source of seat labels: booking_items table (one row per seat)
                let seatLabels = [];
                try {
                    const items = await client.query('SELECT seat_code FROM booking_items WHERE booking_id=$1', [bookingId]);
                    if (items && items.rowCount) {
                        seatLabels = items.rows.map(r => r.seat_code).filter(Boolean);
                    }
                } catch (e) {
                    console.warn('Could not read booking_items for booking', bookingId, e.message || e);
                }

                // If booking_items are empty, fall back to any legacy columns on bookings row (seat_label / seat / seat_labels)
                if ((!seatLabels || seatLabels.length === 0)) {
                    if (booking.seat_label) seatLabels = [booking.seat_label];
                    else if (booking.seat) seatLabels = [booking.seat];
                    else if (booking.seat_labels) {
                        try {
                            if (Array.isArray(booking.seat_labels)) seatLabels = booking.seat_labels;
                            else if (typeof booking.seat_labels === 'string') seatLabels = JSON.parse(booking.seat_labels || '[]');
                            else seatLabels = String(booking.seat_labels).split(',').map(s => s.trim()).filter(Boolean);
                        } catch (e) {
                            console.warn('Could not parse booking.seat_labels for', bookingId, e.message || e);
                            seatLabels = [];
                        }
                    }
                }

                // Release each seat associated with this booking (by seats.label)
                let releasedCount = 0;
                for (const label of seatLabels) {
                    try {
                        const r = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=$1 AND label=$2 RETURNING id', [booking.trip_id, label]);
                        if (r.rowCount) releasedCount += r.rowCount;
                    } catch (e) {
                        console.warn(`Failed to release seat \"${label}\" for booking ${bookingId}: ${e.message || e}`);
                    }
                }

                // If we couldn't determine seats (no booking_items and no legacy seat columns), attempt to release by seats.booking_id
                if (releasedCount === 0) {
                    try {
                        const res2 = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1 RETURNING id', [bookingId]);
                        releasedCount = res2.rowCount || 0;
                    } catch (e) {
                        console.warn('Fallback release by booking_id failed for', bookingId, e.message || e);
                    }
                }

                // Increase trip seats_available by releasedCount (if >0)
                if (releasedCount > 0) {
                    await client.query(
                        'UPDATE trips SET seats_available = seats_available + $1 WHERE id = $2',
                        [releasedCount, booking.trip_id]
                    );
                }

                // Mark booking as expired/cancelled (prefer update with expired_at if column exists) instead of deleting
                // Prefer to set cancelled_at if column exists
                const colCheck = await client.query(`
                  SELECT column_name
                  FROM information_schema.columns
                  WHERE table_name = 'bookings'
                  AND column_name IN ('cancelled_at')
                `);

                if (colCheck.rowCount > 0) {
                  await client.query(
                    "UPDATE bookings SET status=$1, cancelled_at=NOW() WHERE id=$2",
                    [newStatus, bookingId]
                  );
                } else {
                  await client.query(
                    "UPDATE bookings SET status=$1 WHERE id=$2",
                    [newStatus, bookingId]
                  );
                }

                // Emit booking_event to user's room (include seat_labels array)
                io.to(`user_${booking.user_id}`).emit('booking_event', {
                  id: bookingId,
                  trip_id: booking.trip_id,
                  seat_labels: seatLabels,
                  status: newStatus
                });

                await client.query('COMMIT');
                client.release();

                console.log(`Auto-cancelling pending booking id=${bookingId} (status=${newStatus}), type=${isOnlinePayment?'online':'offline'}, released=${releasedCount} seats`);
            } catch (err) {
                console.error('Failed to expire booking id=' + bookingId, err.message || err);
                try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
                client.release();
            }
        }
    } catch (err) {
        console.error('expirePendingBookings failed:', err.message || err);
    }
}

// ====================================================================
// BACKGROUND JOB - handle bookings when arrival_time has passed
// - If booking is pending and NOT paid -> cancel + release seats
// - If booking is confirmed or is paid -> mark completed
// This replaces the old commented-out job and runs at startup + interval
// ====================================================================
const COMPLETE_CHECK_INTERVAL_MS = 60 * 1000; // run every minute

async function processArrivalTimeBookings() {
    try {
        console.log('[CRON] processArrivalTimeBookings running...');
        const sql = `
            SELECT b.id, b.user_id, b.trip_id, b.status, b.metadata, b.payment_method, b.paid_at, b.created_at
            FROM bookings b
            JOIN trips t ON t.id = b.trip_id
            -- Interpret stored departure_time as Asia/Ho_Chi_Minh (local VN time) when comparing to NOW()
            WHERE (t.departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()
            AND b.status IN ('pending','confirmed')
        `;

        const { rows } = await db.query(sql);
        console.log(`[CRON] processArrivalTimeBookings: found ${rows ? rows.length : 0} candidate bookings`);
        if (!rows || rows.length === 0) return;

        for (const r of rows) {
            const bookingId = r.id;
            const client = await db.connect();
            try {
                await client.query('BEGIN');

                // Lock booking row
                const bRes = await client.query('SELECT * FROM bookings WHERE id=$1 FOR UPDATE', [bookingId]);
                if (!bRes.rowCount) {
                    await client.query('ROLLBACK');
                    client.release();
                    console.warn(`[CRON] booking id=${bookingId} disappeared before locking`);
                    continue;
                }
                const booking = bRes.rows[0];

                // Parse metadata safely if it's stored as JSON string
                if (booking.metadata && typeof booking.metadata === 'string') {
                    try {
                        booking.metadata = JSON.parse(booking.metadata);
                    } catch (e) {
                        // keep original string but log parse failure
                        console.warn(`[CRON] booking id=${bookingId} metadata JSON parse failed:`, e.message || e);
                    }
                }

                // Double-check trip arrival_time (in case it changed)
                // Ask DB whether departure_time has passed (avoid JS parsing/timezone issues)
                const tRes = await client.query("SELECT departure_time, NOW() AS now, ((departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh') < NOW()) AS has_passed FROM trips WHERE id=$1", [booking.trip_id]);
                if (!tRes.rowCount) {
                    await client.query('ROLLBACK');
                    client.release();
                    console.warn(`[CRON] booking id=${bookingId} trip missing (trip_id=${booking.trip_id})`);
                    continue;
                }
                const departureTime = tRes.rows[0].departure_time;
                const dbNow = tRes.rows[0].now;
                const hasPassed = tRes.rows[0].has_passed === true || tRes.rows[0].has_passed === 't';
                console.log(`[CRON] booking id=${bookingId} status=${booking.status} departure_time=${departureTime} DB_Now=${dbNow} has_passed=${hasPassed} paid_at=${booking.paid_at}`);

                if (!departureTime) {
                    await client.query('ROLLBACK');
                    client.release();
                    console.warn(`[CRON] booking id=${bookingId} skipped: departure_time is null`);
                    continue;
                }

                if (!hasPassed) {
                    await client.query('ROLLBACK');
                    client.release();
                    console.warn(`[CRON] booking id=${bookingId} skipped: DB reports departure_time not passed (departure_time=${departureTime}, now=${dbNow})`);
                    continue;
                }

                // Determine if booking is considered "paid"
                let isPaid = false;
                // Prefer explicit paid_at column if present
                if (booking.paid_at) isPaid = true;

                // metadata.payment.status or other flags
                if (!isPaid && booking.metadata && booking.metadata.payment) {
                    const mp = booking.metadata.payment;
                    if (mp.status && String(mp.status).toLowerCase() === 'paid') isPaid = true;
                    else if (mp.paid === true) isPaid = true;
                    else if (mp.orderCode && (mp.state && String(mp.state).toLowerCase() === 'paid')) isPaid = true;
                }

                // Also treat already-confirmed bookings as effectively paid/attended
                if (booking.status === 'confirmed') isPaid = true;

                if (isPaid) {
                    console.log(`[CRON] booking id=${bookingId} considered PAID/CONFIRMED -> marking completed`);
                    // Mark as completed
                    try {
                        await client.query("UPDATE bookings SET status='completed' WHERE id=$1", [bookingId]);
                        io.to(`user_${booking.user_id}`).emit('booking_event', {
                            id: bookingId,
                            trip_id: booking.trip_id,
                            status: 'completed'
                        });
                        await client.query('COMMIT');
                        client.release();
                        console.log(`Marked booking id=${bookingId} as completed (departure_time passed)`);
                     } catch (e) {
                         await client.query('ROLLBACK');
                         client.release();
                         console.error('Failed to mark booking completed id=' + bookingId, e.message || e);
                     }
                     continue;
                 }

                 // Not paid -> cancel and release seats
                 console.log(`[CRON] booking id=${bookingId} not paid -> will cancel and release seats`);
                 let seatLabels = [];
                try {
                    const items = await client.query('SELECT seat_code FROM booking_items WHERE booking_id=$1', [bookingId]);
                    if (items && items.rowCount) seatLabels = items.rows.map(r => r.seat_code).filter(Boolean);
                } catch (e) {
                    console.warn('Could not read booking_items for booking', bookingId, e.message || e);
                }

                if ((!seatLabels || seatLabels.length === 0)) {
                    if (booking.seat_label) seatLabels = [booking.seat_label];
                    else if (booking.seat) seatLabels = [booking.seat];
                    else if (booking.seat_labels) {
                        try {
                            if (Array.isArray(booking.seat_labels)) seatLabels = booking.seat_labels;
                            else if (typeof booking.seat_labels === 'string') seatLabels = JSON.parse(booking.seat_labels || '[]');
                            else seatLabels = String(booking.seat_labels).split(',').map(s => s.trim()).filter(Boolean);
                        } catch (e) {
                            console.warn('Could not parse booking.seat_labels for', bookingId, e.message || e);
                            seatLabels = [];
                        }
                    }
                }

                let releasedCount = 0;
                for (const label of seatLabels) {
                    try {
                        const u = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=$1 AND label=$2 RETURNING id', [booking.trip_id, label]);
                        if (u.rowCount) releasedCount += u.rowCount;
                    } catch (e) {
                        console.warn(`Failed to release seat "${label}" for booking ${bookingId}: ${e.message || e}`);
                    }
                }

                if (releasedCount === 0) {
                    try {
                        const res2 = await client.query('UPDATE seats SET is_booked=0, booking_id=NULL WHERE booking_id=$1 RETURNING id', [bookingId]);
                        releasedCount = res2.rowCount || 0;
                    } catch (e) {
                        console.warn('Fallback release by booking_id failed for', bookingId, e.message || e);
                    }
                }

                if (releasedCount > 0) {
                    await client.query('UPDATE trips SET seats_available = seats_available + $1 WHERE id = $2', [releasedCount, booking.trip_id]);
                }

                // Mark booking as cancelled (use cancelled_at if available)
                try {
                    const colCheck = await client.query(`
                      SELECT column_name
                      FROM information_schema.columns
                      WHERE table_name = 'bookings'
                      AND column_name IN ('cancelled_at')
                    `);

                    if (colCheck.rowCount > 0) {
                      await client.query("UPDATE bookings SET status=$1, cancelled_at=NOW() WHERE id=$2", ['cancelled', bookingId]);
                    } else {
                      await client.query("UPDATE bookings SET status=$1 WHERE id=$2", ['cancelled', bookingId]);
                    }

                    io.to(`user_${booking.user_id}`).emit('booking_event', {
                        id: bookingId,
                        trip_id: booking.trip_id,
                        seat_labels: seatLabels,
                        status: 'cancelled'
                    });

                    await client.query('COMMIT');
                    client.release();
                    console.log(`Auto-cancelled pending booking id=${bookingId} (departure_time passed), released=${releasedCount} seats`);
                } catch (e) {
                    await client.query('ROLLBACK');
                    client.release();
                    console.error('Failed to cancel booking id=' + bookingId, e.message || e);
                }
            } catch (err) {
                try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
                client.release();
                console.error('Processing booking on arrival_time failed for id=' + bookingId, err.message || err);
            }
        }
    } catch (err) {
        console.error('processArrivalTimeBookings failed:', err.message || err);
    }
}

// Start intervals
setInterval(expirePendingBookings, EXPIRE_CHECK_INTERVAL_MS);
setInterval(processArrivalTimeBookings, COMPLETE_CHECK_INTERVAL_MS);
// setInterval(completeFinishedBookings, COMPLETE_CHECK_INTERVAL_MS); // Commented out - using cron job instead

// Run once at startup
expirePendingBookings();
processArrivalTimeBookings();
// completeFinishedBookings(); // Commented out - using cron job instead

// Start HTTP server (Express + Socket.IO)
const PORT = process.env.PORT || 10000;

// Run migrations before starting server
(async () => {
  await runMigrations();

  server.listen(PORT, () => {
    console.log("Server + Socket.IO started on port", PORT);
    listRoutes(app);
  });
})();

// Export io for other modules if necessary
module.exports = { io };

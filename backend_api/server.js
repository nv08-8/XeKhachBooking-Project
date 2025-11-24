require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const db = require("./db");

const authRoutes = require("./routes/authRoutes");
const tripRoutes = require("./routes/tripRoutes");
const dataRoutes = require("./routes/dataRoutes");
const bookingRoutes = require("./routes/bookingRoutes");
const promoRoutes = require("./routes/promoRoutes");
const metaRoutes = require("./routes/metaRoutes");
const paymentRoutes = require("./routes/paymentRoutes");
const busImageRoutes = require("./routes/busImageRoutes");
const tripDetailRoutes = require("./routes/tripDetailRoutes");

app.use(express.json());
app.use(cors());

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
app.use("/api", tripDetailRoutes);

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

// FIX PORT CHO RAILWAY
const PORT = process.env.PORT || 10000;

app.listen(PORT, () => {
    console.log("Server started on port", PORT);
    listRoutes(app);
});

// Background job: expire pending bookings older than configured TTL (default 5 minutes)
const BOOKING_PENDING_TTL_MINUTES = parseInt(process.env.BOOKING_PENDING_TTL_MINUTES || '5', 10);
const EXPIRE_CHECK_INTERVAL_MS = 60 * 1000; // run every minute

async function expirePendingBookings() {
    try {
        console.log(`Running expirePendingBookings (TTL=${BOOKING_PENDING_TTL_MINUTES}min)`);
        // Find pending bookings older than TTL
        const sql = `SELECT id, trip_id, seat_label FROM bookings WHERE status='pending' AND created_at < NOW() - INTERVAL '${BOOKING_PENDING_TTL_MINUTES} minutes'`;
        const { rows } = await db.query(sql);
        if (!rows || rows.length === 0) return;

        for (const booking of rows) {
            const client = await db.connect();
            try {
                await client.query('BEGIN');

                // Double-check booking still pending
                const bRes = await client.query('SELECT * FROM bookings WHERE id=$1 FOR UPDATE', [booking.id]);
                if (!bRes.rowCount) {
                    await client.query('ROLLBACK');
                    client.release();
                    continue;
                }
                if (bRes.rows[0].status !== 'pending') {
                    await client.query('ROLLBACK');
                    client.release();
                    continue;
                }

                // Release seat(s) associated with this booking
                // Some schemas may store one seat per booking row (seat_label column)
                await client.query(
                    'UPDATE seats SET is_booked=0, booking_id=NULL WHERE trip_id=$1 AND label=$2',
                    [booking.trip_id, booking.seat_label]
                );

                // Increase trip seats_available by 1
                await client.query(
                    'UPDATE trips SET seats_available = seats_available + 1 WHERE id = $1',
                    [booking.trip_id]
                );

                // Finally delete the booking row
                await client.query('DELETE FROM bookings WHERE id=$1', [booking.id]);

                await client.query('COMMIT');
                client.release();

                console.log(`Expired pending booking id=${booking.id}, trip_id=${booking.trip_id}, seat=${booking.seat_label}`);
            } catch (err) {
                console.error('Failed to expire booking id=' + booking.id, err.message || err);
                try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
                client.release();
            }
        }
    } catch (err) {
        console.error('expirePendingBookings failed:', err.message || err);
    }
}

// Start interval
setInterval(expirePendingBookings, EXPIRE_CHECK_INTERVAL_MS);

// Run once at startup
expirePendingBookings();

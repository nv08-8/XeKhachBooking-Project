// Manual script to verify and update payment status
// Run this if webhook doesn't work: node manual-verify-payment.js <booking_id>

const db = require('./db');

const bookingId = process.argv[2];

if (!bookingId) {
    console.error('Usage: node manual-verify-payment.js <booking_id>');
    process.exit(1);
}

async function verifyAndUpdateBooking() {
    try {
        // Check current booking status
        const { rows } = await db.query('SELECT * FROM bookings WHERE id = $1', [bookingId]);

        if (rows.length === 0) {
            console.error(`❌ Booking ${bookingId} not found`);
            process.exit(1);
        }

        const booking = rows[0];
        console.log('\n📋 Current booking info:');
        console.log('  ID:', booking.id);
        console.log('  Status:', booking.status);
        console.log('  User:', booking.user_id);
        // Fetch seat labels (booking_items) for this booking to support new schema
        try {
            const { rows: items } = await db.query('SELECT seat_code FROM booking_items WHERE booking_id=$1', [bookingId]);
            if (items && items.length) {
                console.log('  Seats:', items.map(i => i.seat_code).join(', '));
            } else {
                console.log('  Seats: (none recorded in booking_items)');
            }
        } catch (e) {
            console.warn('  Could not read booking_items for seats:', e.message || e);
            // fallback: log legacy seat_label if present (for backwards compatibility)
            if (booking.seat_label) console.log('  Seat (legacy):', booking.seat_label);
        }
        console.log('  Price:', booking.price_paid || booking.total_amount);
        console.log('  Created:', booking.created_at);

        if (booking.status === 'confirmed') {
            console.log('\n✅ Booking already confirmed!');
            process.exit(0);
        }

        // Check if there's a payment order for this booking (guard if table missing)
        let orders = [];
        try {
            const q = "SELECT * FROM payment_orders WHERE booking_ids::jsonb @> $1::jsonb ORDER BY created_at DESC LIMIT 1";
            const result = await db.query(q, [`[${bookingId}]`]);
            orders = result.rows || [];
        } catch (err) {
            console.warn('Could not query payment_orders (table may not exist):', err.message || err);
            orders = [];
        }

        if (orders.length > 0) {
            console.log('\n💳 Payment order found:');
            console.log('  Order code:', orders[0].order_code);
            console.log('  Amount:', orders[0].amount);
            console.log('  Created:', orders[0].created_at);

            // Create payment record and update booking
            const amount = Number(orders[0].amount) || Number(booking.total_amount) || 0;
            try {
                const { rows: pRows } = await db.query(
                    'INSERT INTO payments (booking_id, amount, method, transaction_id, status, created_at) VALUES ($1,$2,$3,$4,$5,NOW()) RETURNING id',
                    [bookingId, amount, 'payos', orders[0].order_code || null, 'completed']
                );
                const paymentId = pRows && pRows.length ? pRows[0].id : null;
                await db.query('UPDATE bookings SET status=$1, price_paid=$2, payment_id=$3 WHERE id=$4', ['confirmed', amount, paymentId, bookingId]);
                console.log('✅ Booking updated and payment recorded');
            } catch (e) {
                console.error('Failed to create payment or update booking:', e.message || e);
            }
        } else {
            console.log('\n⚠️  No payment order found for this booking (or payment_orders missing)');
            console.log('💡 Manual update: Marking booking as paid (cash) and creating payment record...');
            const amount = Number(booking.total_amount) || Number(booking.price_paid) || 0;
            try {
                const { rows: pRows } = await db.query(
                    'INSERT INTO payments (booking_id, amount, method, transaction_id, status, created_at) VALUES ($1,$2,$3,$4,$5,NOW()) RETURNING id',
                    [bookingId, amount, 'cash', null, 'completed']
                );
                const paymentId = pRows && pRows.length ? pRows[0].id : null;
                await db.query('UPDATE bookings SET status=$1, price_paid=$2, payment_id=$3 WHERE id=$4', ['confirmed', amount, paymentId, bookingId]);
                console.log('✅ Booking updated as cash payment');
            } catch (e) {
                console.error('Failed to create cash payment or update booking:', e.message || e);
            }
        }

        // Verify update
        const { rows: updated } = await db.query('SELECT * FROM bookings WHERE id = $1', [bookingId]);
        console.log('\n📋 Updated booking info:');
        console.log('  Status:', updated[0].status);
        console.log('  Price paid:', updated[0].price_paid);
        console.log('  Payment id:', updated[0].payment_id);

        process.exit(0);
    } catch (err) {
        console.error('❌ Error:', err.message);
        process.exit(1);
    }
}

verifyAndUpdateBooking();

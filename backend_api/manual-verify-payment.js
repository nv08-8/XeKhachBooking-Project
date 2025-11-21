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
        console.log('  Seat:', booking.seat_label);
        console.log('  Price:', booking.price_paid);
        console.log('  Payment method:', booking.payment_method);
        console.log('  Created:', booking.created_at);

        if (booking.status === 'confirmed') {
            console.log('\n✅ Booking already confirmed!');
            process.exit(0);
        }

        // Check if there's a payment order for this booking
        const { rows: orders } = await db.query(
            "SELECT * FROM payment_orders WHERE booking_ids::jsonb @> $1::jsonb ORDER BY created_at DESC LIMIT 1",
            [`[${bookingId}]`]
        );

        if (orders.length > 0) {
            console.log('\n💳 Payment order found:');
            console.log('  Order code:', orders[0].order_code);
            console.log('  Amount:', orders[0].amount);
            console.log('  Created:', orders[0].created_at);

            // Update booking to confirmed
            console.log('\n🔄 Updating booking status to confirmed...');
            await db.query(
                "UPDATE bookings SET status='confirmed', payment_method='payos', payment_time=NOW() WHERE id=$1",
                [bookingId]
            );
            console.log('✅ Booking updated successfully!');
        } else {
            console.log('\n⚠️  No payment order found for this booking');
            console.log('💡 Manual update: Updating to confirmed anyway...');
            await db.query(
                "UPDATE bookings SET status='confirmed', payment_method='cash', payment_time=NOW() WHERE id=$1",
                [bookingId]
            );
            console.log('✅ Booking updated as cash payment!');
        }

        // Verify update
        const { rows: updated } = await db.query('SELECT * FROM bookings WHERE id = $1', [bookingId]);
        console.log('\n📋 Updated booking info:');
        console.log('  Status:', updated[0].status);
        console.log('  Payment method:', updated[0].payment_method);
        console.log('  Payment time:', updated[0].payment_time);

        process.exit(0);
    } catch (err) {
        console.error('❌ Error:', err.message);
        process.exit(1);
    }
}

verifyAndUpdateBooking();


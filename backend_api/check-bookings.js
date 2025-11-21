// Quick script to check recent bookings
const db = require('./db');

async function checkRecentBookings() {
    try {
        const { rows } = await db.query(
            "SELECT id, user_id, status, seat_label, price_paid, payment_method, created_at FROM bookings ORDER BY created_at DESC LIMIT 10"
        );

        console.log('\n📋 Recent 10 bookings:');
        console.log('═══════════════════════════════════════════════════════════════');
        rows.forEach(b => {
            const status = b.status === 'pending' ? '⏳' : b.status === 'confirmed' ? '✅' : '❌';
            console.log(`${status} ID: ${b.id} | User: ${b.user_id} | Seat: ${b.seat_label} | Status: ${b.status} | Payment: ${b.payment_method || 'null'} | ${b.created_at}`);
        });
        console.log('═══════════════════════════════════════════════════════════════\n');

        process.exit(0);
    } catch (err) {
        console.error('❌ Error:', err.message);
        process.exit(1);
    }
}

checkRecentBookings();


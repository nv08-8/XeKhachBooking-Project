const cron = require("node-cron");
const db = require("../db");

// Will be set by server.js after io is initialized
let io = null;

/**
 * Set Socket.IO instance for real-time notifications
 */
function setSocketIO(socketIO) {
  io = socketIO;
  console.log("✅ Socket.IO connected to booking status cron job");
}

/**
 * Cron Job: Auto-complete bookings
 * Runs every 5 minutes to check and update booking status to 'completed'
 * when the trip's arrival_time has passed
 */
cron.schedule("*/5 * * * *", async () => {
  try {
    console.log("\n🕐 [CRON] Checking for bookings to auto-complete...");

    // Find bookings with status='confirmed' where trip arrival_time < NOW()
    const selectQuery = `
      SELECT b.id, b.user_id, b.trip_id, b.status, t.arrival_time
      FROM bookings b
      JOIN trips t ON t.id = b.trip_id
      WHERE b.status = 'confirmed'
        AND t.arrival_time < NOW()
    `;

    const { rows: bookingsToComplete } = await db.query(selectQuery);

    if (bookingsToComplete.length === 0) {
      console.log("   ℹ️  No bookings to complete");
      return;
    }

    console.log(`   📋 Found ${bookingsToComplete.length} booking(s) to complete`);

    // Update each booking to 'completed'
    let successCount = 0;
    let failCount = 0;

    for (const booking of bookingsToComplete) {
      try {
        await db.query(
          "UPDATE bookings SET status = 'completed' WHERE id = $1",
          [booking.id]
        );

        console.log(`   ✅ Booking #${booking.id} -> completed (arrival: ${new Date(booking.arrival_time).toLocaleString()})`);
        successCount++;

        // Emit socket event to user for real-time update
        if (io) {
          io.to(`user_${booking.user_id}`).emit('booking_event', {
            id: booking.id,
            trip_id: booking.trip_id,
            status: 'completed'
          });
        }
      } catch (err) {
        console.error(`   ❌ Failed to complete booking #${booking.id}:`, err.message);
        failCount++;
      }
    }

    console.log(`   📊 Summary: ${successCount} succeeded, ${failCount} failed`);

  } catch (err) {
    console.error("❌ [CRON ERROR] Auto-complete job failed:", err.message);
  }
});

console.log("✅ Cron job initialized: Auto-complete bookings every 5 minutes");

module.exports = { setSocketIO };


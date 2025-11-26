const db = require('../db');
const { generateAndCacheSeats } = require('../utils/seatGenerator');

async function regenerateAll() {
  console.log('Starting seat regeneration for all trips...');
  try {
    const { rows: trips } = await db.query('SELECT id FROM trips');
    console.log(`Found ${trips.length} trips`);
    for (const t of trips) {
      const client = await db.connect();
      try {
        await client.query('BEGIN');
        const res = await generateAndCacheSeats(client, t.id);
        await client.query('COMMIT');
        client.release();
        console.log(`Trip ${t.id}: generated ${res.created_count || 0} seats`);
      } catch (err) {
        console.error(`Trip ${t.id}: regeneration failed:`, err.message || err);
        try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
        client.release();
      }
    }
    console.log('Seat regeneration completed');
    process.exit(0);
  } catch (err) {
    console.error('Failed to list trips:', err.message || err);
    process.exit(1);
  }
}

if (require.main === module) {
  regenerateAll();
}

module.exports = { regenerateAll };

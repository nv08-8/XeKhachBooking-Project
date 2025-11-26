#!/usr/bin/env node
require('dotenv').config();
const db = require('../db');
const { generateAndCacheSeats } = require('../utils/seatGenerator');

async function regenerateSafe() {
  console.log('Starting SAFE seat regeneration: only for trips without bookings');
  try {
    const { rows: trips } = await db.query('SELECT id FROM trips');
    console.log(`Found ${trips.length} trips`);
    for (const t of trips) {
      const client = await db.connect();
      try {
        // check bookings
        const b = await client.query('SELECT 1 FROM bookings WHERE trip_id=$1 LIMIT 1', [t.id]);
        if (b.rowCount > 0) {
          console.log(`Trip ${t.id}: has existing bookings -> SKIP regeneration to avoid breaking bookings`);
          client.release();
          continue;
        }

        await client.query('BEGIN');
        // delete any existing seat rows for this trip (safe because no bookings)
        await client.query('DELETE FROM seats WHERE trip_id=$1', [t.id]);
        const res = await generateAndCacheSeats(client, t.id);
        await client.query('COMMIT');
        client.release();
        console.log(`Trip ${t.id}: regenerated ${res.created_count || 0} seats (no bookings)`);
      } catch (err) {
        console.error(`Trip ${t.id}: safe regeneration failed:`, err.message || err);
        try { await client.query('ROLLBACK'); } catch (e) { /* ignore */ }
        client.release();
      }
    }
    console.log('SAFE seat regeneration completed');
    process.exit(0);
  } catch (err) {
    console.error('Failed to list trips for safe regeneration:', err.message || err);
    process.exit(1);
  }
}

if (require.main === module) {
  regenerateSafe();
}

module.exports = { regenerateSafe };

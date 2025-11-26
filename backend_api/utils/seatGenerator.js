const db = require('../db');

/**
 * Generates seat records for a given trip based on the seat_layout of its associated bus.
 * This function is idempotent; it will not create duplicates if seats already exist.
 * @param {number} tripId The ID of the trip to generate seats for.
 * @returns {Promise<{created_count: number}>} A summary of the operation.
 */
async function generateAndCacheSeats(tripId) {
  const client = await db.connect();
  try {
    await client.query('BEGIN');

    const tripResult = await client.query(
      `SELECT t.bus_id, b.seat_layout 
       FROM trips t 
       JOIN buses b ON b.id = t.bus_id 
       WHERE t.id = $1`,
      [tripId]
    );

    if (tripResult.rowCount === 0 || !tripResult.rows[0].seat_layout) {
      // This case is not an error, just a trip without a layout.
      await client.query('ROLLBACK'); // Abort transaction
      return { created_count: 0, message: 'No seat layout for trip.' };
    }

    let layout = tripResult.rows[0].seat_layout;
    if (typeof layout === 'string') {
      layout = JSON.parse(layout);
    }

    const seatsToCreate = [];
    if (layout && Array.isArray(layout.floors)) {
      for (const floor of layout.floors) {
        if (Array.isArray(floor.seats)) {
          for (const seat of floor.seats) {
            if (seat.label && seat.type !== 'aisle') {
              seatsToCreate.push({ label: seat.label, type: seat.type || 'seat' });
            }
          }
        }
      }
    }

    if (seatsToCreate.length === 0) {
      await client.query('ROLLBACK');
      return { created_count: 0 };
    }

    let created_count = 0;
    for (const seat of seatsToCreate) {
      const insertResult = await client.query(
        'INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1, $2, $3, 0) ON CONFLICT (trip_id, label) DO NOTHING',
        [tripId, seat.label, seat.type]
      );
      if (insertResult.rowCount > 0) {
        created_count++;
      }
    }

    await client.query('COMMIT');
    return { created_count };

  } catch (err) {
    await client.query('ROLLBACK');
    console.error(`Error generating seats for trip ${tripId}:`, err.message);
    throw err;
  } finally {
    client.release();
  }
}

module.exports = { generateAndCacheSeats };

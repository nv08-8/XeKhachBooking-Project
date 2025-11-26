const db = require('../db');

/**
 * Generates seat records for a given trip using a provided database client.
 * This function operates within an existing transaction and does not commit, rollback, or release the client.
 * @param {object} client - The database client from a pre-existing connection.
 * @param {number} tripId - The ID of the trip to generate seats for.
 * @returns {Promise<{created_count: number}>} A summary of the operation.
 */
async function generateAndCacheSeats(client, tripId) {
  try {
    // First, check if seats already exist to avoid unnecessary work.
    const seatCheck = await client.query('SELECT 1 FROM seats WHERE trip_id = $1 LIMIT 1', [tripId]);
    if (seatCheck.rowCount > 0) {
      return { created_count: 0, message: 'Seats already exist.' };
    }

    const tripResult = await client.query(
      `SELECT b.seat_layout FROM trips t JOIN buses b ON b.id = t.bus_id WHERE t.id = $1`,
      [tripId]
    );

    if (tripResult.rowCount === 0 || !tripResult.rows[0].seat_layout) {
      console.warn(`Trip or seat layout not found for trip_id: ${tripId}. No seats generated.`);
      return { created_count: 0 };
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
      return { created_count: 0 };
    }

    let created_count = 0;
    for (const seat of seatsToCreate) {
      // Use integer 0 for is_booked status, which corresponds to FALSE in PostgreSQL boolean context
      const insertResult = await client.query(
        'INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1, $2, $3, 0) ON CONFLICT (trip_id, label) DO NOTHING',
        [tripId, seat.label, seat.type]
      );
      if (insertResult.rowCount > 0) {
        created_count++;
      }
    }
    console.log(`Successfully generated ${created_count} seats for trip ${tripId}`);
    return { created_count };
  } catch (err) {
    console.error(`FATAL: Error during seat generation for trip ${tripId} inside a transaction:`, err.message);
    // Re-throw the error so the calling function can roll back the entire transaction.
    throw err;
  }
}

module.exports = { generateAndCacheSeats };

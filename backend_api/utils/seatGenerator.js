const db = require('../db');

/**
 * Generates seat records for a given trip using a provided database client.
 * This function operates within an existing transaction and does not commit or rollback.
 * @param {object} client - The database client from a pre-existing connection.
 * @param {number} tripId - The ID of the trip to generate seats for.
 * @returns {Promise<{created_count: number}>} A summary of the operation.
 */
async function generateAndCacheSeats(client, tripId) {
  try {
    const tripResult = await client.query(
      `SELECT t.bus_id, b.seat_layout 
       FROM trips t 
       JOIN buses b ON b.id = t.bus_id 
       WHERE t.id = $1`,
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
      const insertResult = await client.query(
        'INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1, $2, $3, 0) ON CONFLICT (trip_id, label) DO NOTHING',
        [tripId, seat.label, seat.type]
      );
      if (insertResult.rowCount > 0) {
        created_count++;
      }
    }
    return { created_count };
  } catch (err) {
    console.error(`FATAL: Error during seat generation for trip ${tripId} inside a transaction:`, err.message);
    // We must re-throw the error so the calling function can roll back the entire transaction.
    throw err;
  }
}

module.exports = { generateAndCacheSeats };

const db = require('../db');
const { generateDetailedSeatLayout } = require('../data/seat_layout.js');

/**
 * Generates seat records for a given trip using a provided database client.
 */
async function generateAndCacheSeats(client, tripId) {
  try {
    const seatCheck = await client.query('SELECT 1 FROM seats WHERE trip_id = $1 LIMIT 1', [tripId]);
    if (seatCheck.rowCount > 0) {
      return { created_count: 0, message: 'Seats already exist.' };
    }

    // Lấy layout linh hoạt: Ưu tiên bus_id, fallback sang bus_type
    const tripResult = await client.query(
      `SELECT t.bus_type, 
              COALESCE(
                (SELECT b1.seat_layout FROM buses b1 WHERE b1.id = t.bus_id),
                (SELECT b2.seat_layout FROM buses b2 WHERE LOWER(REPLACE(b2.bus_type, ' ', '')) = LOWER(REPLACE(t.bus_type, ' ', '')) LIMIT 1)
              ) as seat_layout
       FROM trips t WHERE t.id = $1`,
      [tripId]
    );

    if (tripResult.rowCount === 0 || !tripResult.rows[0].seat_layout) {
      console.warn(`[SeatGenerator] Không tìm thấy layout cho trip: ${tripId}`);
      return { created_count: 0 };
    }

    let layout = tripResult.rows[0].seat_layout;
    const busType = tripResult.rows[0].bus_type || '';
    if (typeof layout === 'string') layout = JSON.parse(layout);

    // Sinh chi tiết ghế từ layout thô
    layout = generateDetailedSeatLayout(busType, layout);

    const seatsToCreate = [];
    if (layout && Array.isArray(layout.floors)) {
      for (let fi = 0; fi < layout.floors.length; fi++) {
        const floor = layout.floors[fi];
        const floorIndex = floor.floor || (fi + 1);
        if (Array.isArray(floor.seats)) {
          for (const seat of floor.seats) {
            if (seat.label && seat.type !== 'aisle') {
              seatsToCreate.push({ label: seat.label, type: seat.type || 'seat', floor: floorIndex });
            }
          }
        }
      }
    }

    if (seatsToCreate.length === 0) return { created_count: 0 };

    const colCheck = await client.query(`SELECT column_name FROM information_schema.columns WHERE table_name = 'seats' AND column_name = 'floor'`);
    const hasFloor = colCheck.rowCount > 0;

    let created_count = 0;
    for (const seat of seatsToCreate) {
      const query = hasFloor 
        ? 'INSERT INTO seats (trip_id, label, type, is_booked, floor) VALUES ($1, $2, $3, 0, $4) ON CONFLICT DO NOTHING'
        : 'INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1, $2, $3, 0) ON CONFLICT DO NOTHING';
      const params = hasFloor ? [tripId, seat.label, seat.type, seat.floor] : [tripId, seat.label, seat.type];
      
      const insertResult = await client.query(query, params);
      if (insertResult.rowCount > 0) created_count++;
    }
    
    return { created_count };
  } catch (err) {
    console.error(`[SeatGenerator] Lỗi:`, err.message);
    throw err;
  }
}

module.exports = { generateAndCacheSeats };

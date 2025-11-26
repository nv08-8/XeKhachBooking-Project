const express = require('express');
const router = express.Router();
const db = require('../db');
const { generateAndCacheSeats } = require('../utils/seatGenerator');

// GET /api/seats?trip_id=&status=
router.get('/', async (req, res) => {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');

  const { trip_id, status } = req.query;
  if (!trip_id) {
    return res.status(400).json({ message: 'Missing trip_id' });
  }

  const tripIdNum = parseInt(trip_id, 10);
  if (isNaN(tripIdNum)) {
    return res.status(400).json({ message: 'Invalid trip_id' });
  }

  const client = await db.connect();
  try {
    // The seat generator now handles its own transaction, so we just call it.
    await generateAndCacheSeats(client, tripIdNum);

    let query = 'SELECT id, trip_id, label, type, is_booked, booking_id FROM seats WHERE trip_id=$1';
    const params = [tripIdNum];

    if (status === 'booked') {
      query += ' AND is_booked = 1';
    } else if (status === 'available') {
      query += ' AND (is_booked = 0 OR is_booked IS NULL)';
    }
    query += ' ORDER BY LENGTH(label), label';

    const { rows } = await client.query(query, params);

    const formatted = rows.map(r => ({
      id: r.id,
      trip_id: r.trip_id,
      label: r.label,
      type: r.type,
      is_booked: r.is_booked ? 1 : 0,
      booking_id: r.booking_id,
    }));

    res.json(formatted);

  } catch (err) {
    console.error(`Failed to fetch seats for trip ${tripIdNum}:`, err.message || err);
    res.status(500).json({ message: 'Failed to fetch or generate seats' });
  } finally {
    client.release();
  }
});

module.exports = router;

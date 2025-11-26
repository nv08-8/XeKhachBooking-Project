const express = require('express');
const router = express.Router();
const db = require('../db');
const { generateAndCacheSeats } = require('../utils/seatGenerator');
const { generateDetailedSeatLayout } = require('../data/seat_layout.js');

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

    let query = 'SELECT id, trip_id, label, type, is_booked, booking_id, ';
    // Detect if floor column exists
    const colCheck = await db.query(`SELECT column_name FROM information_schema.columns WHERE table_name = 'seats' AND column_name = 'floor'`);
    const hasFloor = colCheck.rowCount > 0;
    if (hasFloor) query += 'floor ';
    else query = 'SELECT id, trip_id, label, type, is_booked, booking_id ';

    query += 'FROM seats WHERE trip_id=$1';
    const params = [tripIdNum];

    if (status === 'booked') {
      query += ' AND is_booked = 1';
    } else if (status === 'available') {
      query += ' AND (is_booked = 0 OR is_booked IS NULL)';
    }
    query += ' ORDER BY LENGTH(label), label';

    const { rows } = await client.query(query, params);

    // If there are duplicate labels (same label appearing multiple times across floors),
    // and we have a multi-floor layout, remap labels in the response only to use floor prefixes A/B/... accordingly.
    const seatLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let displayMap = null; // Map seatId -> displayLabel

    // detect duplicates
    const labelCounts = rows.reduce((acc, r) => {
      const key = String(r.label || '').trim();
      if (!key) return acc;
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    const hasDuplicates = Object.values(labelCounts).some(c => c > 1);

    if (hasDuplicates) {
      try {
        const tripLayoutRes = await client.query('SELECT t.bus_type, b.seat_layout FROM trips t JOIN buses b ON b.id = t.bus_id WHERE t.id = $1', [tripIdNum]);
        if (tripLayoutRes.rowCount && tripLayoutRes.rows[0].seat_layout) {
          let layout = tripLayoutRes.rows[0].seat_layout;
          const busType = tripLayoutRes.rows[0].bus_type || '';
          if (typeof layout === 'string') layout = JSON.parse(layout);

          if (layout && Array.isArray(layout.floors) && layout.floors.length > 1) {
            // expand to detailed layout so we know floor count
            try {
              layout = generateDetailedSeatLayout(busType, layout);
            } catch (e) {
              // ignore expansion errors and fallback to default behavior
              layout = layout;
            }

            const floorsCount = layout.floors.length;

            // Group DB rows by numeric suffix (e.g., 'A2' -> '2'); fallback to unique key per id if no digits
            const suffixMap = new Map();
            for (const r of rows) {
              const m = String(r.label || '').match(/(\d+)$/);
              const key = m ? m[1] : `__id_${r.id}`;
              if (!suffixMap.has(key)) suffixMap.set(key, []);
              suffixMap.get(key).push(r);
            }

            displayMap = new Map();
            // Assign rows with same suffix to floors sequentially (lower floor -> higher)
            for (const [suffix, arr] of suffixMap.entries()) {
              for (let i = 0; i < arr.length; i++) {
                const seatRow = arr[i];
                const floorIndex = i % floorsCount; // assign in order; wrap if more duplicates than floors
                const prefix = seatLetters[floorIndex] || seatLetters[0];
                const displayLabel = (suffix.startsWith('__id_')) ? `${prefix}${i + 1}` : `${prefix}${suffix}`;
                displayMap.set(seatRow.id, displayLabel);
              }
            }
          }
        }
      } catch (e) {
        console.warn('Failed to load layout for seat label remapping:', e.message || e);
      }
    }

    const formatted = rows.map(r => ({
      id: r.id,
      trip_id: r.trip_id,
      label: displayMap && displayMap.has(r.id) ? displayMap.get(r.id) : r.label,
      type: r.type,
      is_booked: r.is_booked ? 1 : 0,
      booking_id: r.booking_id,
      floor: hasFloor ? r.floor : undefined,
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

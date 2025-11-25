const express = require('express');
const router = express.Router();
const db = require('../db');

// GET /api/seats?trip_id=
router.get('/', async (req, res) => {
  const { trip_id } = req.query;
  if (!trip_id) return res.status(400).json({ message: 'Missing trip_id' });
  try {
    const { rows } = await db.query(
      `SELECT id, trip_id, label, type, is_booked, booking_id
       FROM seats WHERE trip_id=$1 ORDER BY LENGTH(label), label`,
      [trip_id]
    );

    // Normalize is_booked to boolean for clients
    const formatted = rows.map(r => ({
      id: r.id,
      trip_id: r.trip_id,
      label: r.label,
      type: r.type,
      is_booked: r.is_booked === 1 || r.is_booked === true,
      booking_id: r.booking_id
    }));

    res.json(formatted);
  } catch (err) {
    console.error('Failed to fetch seats:', err.message || err);
    res.status(500).json({ message: 'Failed to fetch seats' });
  }
});

// POST /api/seats (admin) - create seats for a trip
// body: { trip_id, seats: [{ label, type }] }
router.post('/', async (req, res) => {
  const { trip_id, seats } = req.body;
  if (!trip_id || !Array.isArray(seats) || seats.length === 0) return res.status(400).json({ message: 'Missing trip_id or seats' });
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const created = [];
    for (const s of seats) {
      const label = s.label;
      const type = s.type || 'seat';
      try {
        const insert = await client.query('INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1,$2,$3,0) RETURNING id, label', [trip_id, label, type]);
        created.push(insert.rows[0]);
      } catch (e) {
        // ignore duplicates
        if (e.code === '23505') continue;
        throw e;
      }
    }
    await client.query('COMMIT');
    res.json({ created_count: created.length, created });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Failed to create seats:', err.message || err);
    res.status(500).json({ message: 'Failed to create seats' });
  } finally {
    client.release();
  }
});

// POST /api/seats/generate-from-bus
// body: { bus_id, trip_id? }
router.post('/generate-from-bus', async (req, res) => {
  const { bus_id, trip_id } = req.body;
  if (!bus_id) return res.status(400).json({ message: 'Missing bus_id' });

  try {
    // Read bus seat_layout
    const { rows: busRows } = await db.query('SELECT id, seat_layout FROM buses WHERE id=$1', [bus_id]);
    if (!busRows || busRows.length === 0) return res.status(404).json({ message: 'Bus not found' });
    let layout = busRows[0].seat_layout;
    if (!layout) return res.status(400).json({ message: 'Bus has no seat_layout' });
    if (typeof layout === 'string') {
      try { layout = JSON.parse(layout); } catch (e) { return res.status(400).json({ message: 'Invalid seat_layout JSON' }); }
    }

    // Build labels from layout
    const labels = [];
    const floors = Array.isArray(layout.floors) ? layout.floors : [];
    for (const f of floors) {
      const floorNum = f.floor || (floors.indexOf(f) + 1);
      const rows = Number(f.rows) || 0;
      const cols = Number(f.cols) || 0;
      const extra_end = Number(f.extra_end_bed || (f.special && f.special.extra_near_end) || 0) || 0;
      const extra_front = Number(f.special && f.special.extra_front_beds || 0) || 0;

      for (let r = 1; r <= rows; r++) {
        let rowLetter;
        if (r <= 26) rowLetter = String.fromCharCode(64 + r); else rowLetter = 'R' + r;
        for (let c = 1; c <= cols; c++) {
          // label format: A1, A2... per floor we prefix with floor when multiple floors are present
          const label = floors.length > 1 ? `F${floorNum}-${rowLetter}${c}` : `${rowLetter}${c}`;
          labels.push(label);
        }
      }
      // extra front beds
      for (let n = 1; n <= extra_front; n++) labels.push(floors.length > 1 ? `F${floorNum}-FRONT${n}` : `FRONT${n}`);
      // extra end beds / benches
      for (let n = 1; n <= extra_end; n++) labels.push(floors.length > 1 ? `F${floorNum}-END${n}` : `END${n}`);
    }

    if (labels.length === 0) return res.status(400).json({ message: 'No seats generated from layout' });

    // Determine target trips
    let trips = [];
    if (trip_id) {
      const { rows } = await db.query('SELECT id, bus_id FROM trips WHERE id=$1', [trip_id]);
      if (!rows.length) return res.status(404).json({ message: 'Trip not found' });
      if (rows[0].bus_id !== bus_id) return res.status(400).json({ message: 'Trip does not use provided bus_id' });
      trips = [rows[0]];
    } else {
      const { rows } = await db.query('SELECT id FROM trips WHERE bus_id=$1', [bus_id]);
      trips = rows || [];
    }

    const summary = [];
    for (const t of trips) {
      const client = await db.connect();
      try {
        await client.query('BEGIN');
        let created = 0;
        for (const label of labels) {
          try {
            await client.query('INSERT INTO seats (trip_id, label, type, is_booked) VALUES ($1,$2,$3,0)', [t.id, label, 'seat']);
            created++;
          } catch (e) {
            if (e.code === '23505') continue; // duplicate
            throw e;
          }
        }
        await client.query('COMMIT');
        summary.push({ trip_id: t.id, created });
      } catch (e) {
        await client.query('ROLLBACK');
        summary.push({ trip_id: t.id, error: e.message || e });
      } finally {
        client.release();
      }
    }

    res.json({ bus_id, trips_processed: summary.length, summary });
  } catch (err) {
    console.error('Failed to generate seats from bus layout:', err.message || err);
    res.status(500).json({ message: 'Failed to generate seats' });
  }
});

module.exports = router;

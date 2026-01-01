/**
 * Migration: Mở rộng seat_layout trong bảng buses với chi tiết ghế
 * Chỉ cần chạy 1 lần để update tất cả buses
 *
 * Usage: node expand_seat_layouts.js
 */

const db = require('../db');
const { generateDetailedSeatLayout } = require('../data/seat_layout');

async function expandSeatLayouts() {
  const client = await db.connect();
  try {
    console.log('🔄 Bắt đầu mở rộng seat_layout cho tất cả buses...');

    // Lấy tất cả buses
    const result = await client.query('SELECT id, bus_type, seat_layout FROM buses ORDER BY id');
    const buses = result.rows;

    console.log(`📋 Tổng cộng ${buses.length} buses cần xử lý`);

    let updated = 0;
    for (const bus of buses) {
      try {
        let layout = bus.seat_layout;

        // Parse nếu là string
        if (typeof layout === 'string') {
          layout = JSON.parse(layout);
        }

        // Kiểm tra nếu layout đã có seats detail
        const hasSeatsDetail = layout?.floors?.some(f =>
          Array.isArray(f.seats) && f.seats.length > 0
        );

        if (hasSeatsDetail) {
          console.log(`✅ Bus ${bus.id} (${bus.bus_type}) - already has seats detail`);
          continue;
        }

        // Expand layout
        const expandedLayout = generateDetailedSeatLayout(bus.bus_type, layout);
        const expandedLayoutJson = JSON.stringify(expandedLayout);

        // Update database
        await client.query(
          'UPDATE buses SET seat_layout = $1 WHERE id = $2',
          [expandedLayoutJson, bus.id]
        );

        updated++;
        console.log(`✅ Bus ${bus.id} (${bus.bus_type}) - expanded with ${
          expandedLayout.floors.reduce((sum, f) => sum + (f.seats?.length || 0), 0)
        } seat details`);

      } catch (e) {
        console.error(`❌ Error updating bus ${bus.id}:`, e.message);
      }
    }

    console.log(`\n✨ Migration hoàn tất! Cập nhật ${updated}/${buses.length} buses`);

  } catch (err) {
    console.error('❌ Migration failed:', err);
    process.exit(1);
  } finally {
    client.release();
    process.exit(0);
  }
}

expandSeatLayouts();


const { generateDetailedSeatLayout } = require('../data/seat_layout');

function makeSampleLayout() {
  return {
    floors: [
      { floor: 1, rows: 3, cols: 3 },
      { floor: 2, rows: 3, cols: 3 }
    ]
  };
}

const layout = makeSampleLayout();
const detailed = generateDetailedSeatLayout('Giường nằm 40 chỗ', layout);
console.log(JSON.stringify(detailed, null, 2));

// Print labels by floor
for (const f of detailed.floors) {
  console.log('Floor', f.floor || 'n/a');
  console.log(f.seats.map(s => s.label).join(', '));
}


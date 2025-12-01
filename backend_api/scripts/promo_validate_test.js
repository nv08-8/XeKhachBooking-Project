const http = require('http');

const cases = [
  // Using promo codes that exist in DB: SALE10 (percent, min_price 100000, max_discount 30000),
  // GIAM20K (fixed 20000, min_price 150000), KM30 (percent 30, min_price 50000, max_discount 50000)
  { name: 'valid_percent_sale10', body: { code: 'SALE10', amount: 500000 } },
  { name: 'below_min_price_sale10', body: { code: 'SALE10', amount: 5000 } },
  { name: 'fixed_amount_giam20k', body: { code: 'GIAM20K', amount: 200000 } },
  { name: 'percent_km30', body: { code: 'KM30', amount: 100000 } },
];

function runCase(c, cb) {
  const payload = JSON.stringify(c.body);
  const opts = {
    hostname: 'localhost',
    port: 10000, // server default in server.js
    path: '/api/promotions/validate',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(payload)
    }
  };

  const req = http.request(opts, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
      console.log('--- CASE:', c.name, '---');
      console.log('Status:', res.statusCode);
      try { console.log('Body:', JSON.parse(data)); } catch (e) { console.log('BodyRaw:', data); }
      cb();
    });
  });

  req.on('error', (err) => {
    console.error('Request error for case', c.name, err.message);
    cb();
  });

  req.write(payload);
  req.end();
}

(function runAll(i) {
  if (i >= cases.length) return;
  runCase(cases[i], () => runAll(i + 1));
})(0);

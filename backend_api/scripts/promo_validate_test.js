const http = require('http');

const cases = [
  { name: 'valid_percent', body: { code: 'SUMMER10', amount: 500000 } },
  { name: 'below_min_price', body: { code: 'SUMMER10', amount: 5000 } },
  { name: 'fixed_amount', body: { code: 'FLAT50K', amount: 200000 } },
  { name: 'expired', body: { code: 'OLD', amount: 200000 } },
];

function runCase(c, cb) {
  const payload = JSON.stringify(c.body);
  const opts = {
    hostname: 'localhost',
    port: 3000,
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


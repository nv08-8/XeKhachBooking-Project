const http = require('http');

const payload = JSON.stringify({ code: 'SUMMER10', amount: 500000 });

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
  res.on('data', (chunk) => (data += chunk));
  res.on('end', () => {
    console.log('Response status:', res.statusCode);
    try {
      console.log('Body:', JSON.parse(data));
    } catch (e) {
      console.log('Body (raw):', data);
    }
  });
});

req.on('error', (err) => {
  console.error('Request error:', err);
});

req.write(payload);
req.end();


// Test script to verify /api/trips/:id/seats endpoint
const http = require('http');

// Replace with your backend URL
const tripId = 1; // Replace with valid trip ID
const backendUrl = 'http://localhost:3000';

const options = {
  hostname: 'localhost',
  port: 3000,
  path: `/api/trips/${tripId}/seats`,
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
};

const req = http.request(options, (res) => {
  console.log(`Status: ${res.statusCode}`);
  console.log(`Headers:`, res.headers);

  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    console.log('Response body:');
    console.log(data);

    try {
      const parsed = JSON.parse(data);
      console.log('\nParsed JSON:');
      console.log(JSON.stringify(parsed, null, 2));

      if (parsed.length > 0) {
        console.log('\nFirst seat:');
        console.log(parsed[0]);
      }
    } catch (e) {
      console.error('Failed to parse JSON:', e.message);
    }
  });
});

req.on('error', (e) => {
  console.error(`Problem with request: ${e.message}`);
});

req.end();


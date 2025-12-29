// Test script to check if seats endpoint works
// Run with: node test-seats-api.js

const http = require('http');

// Test data - replace with your actual trip ID
const tripId = 1;
const backendUrl = 'localhost:3000';

console.log(`Testing /api/trips/${tripId}/seats endpoint...\n`);

// Test 1: Get all seats
testEndpoint(`/api/trips/${tripId}/seats`, 'GET all seats');

// Test 2: Get only available seats
testEndpoint(`/api/trips/${tripId}/seats?available=true`, 'GET available seats only');

function testEndpoint(path, description) {
  console.log(`\n📝 Test: ${description}`);
  console.log(`Path: ${path}\n`);

  const options = {
    hostname: 'localhost',
    port: 3000,
    path: path,
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  };

  const req = http.request(options, (res) => {
    console.log(`Status: ${res.statusCode}`);

    let data = '';
    res.on('data', (chunk) => {
      data += chunk;
    });

    res.on('end', () => {
      console.log('Response:');

      if (res.statusCode === 200) {
        try {
          const parsed = JSON.parse(data);
          console.log(JSON.stringify(parsed, null, 2));

          if (Array.isArray(parsed)) {
            console.log(`\n✅ Returned ${parsed.length} seats`);
            if (parsed.length > 0) {
              console.log('First seat:', parsed[0]);
            }
          } else {
            console.log('❌ Response is not an array!');
          }
        } catch (e) {
          console.error('❌ Failed to parse JSON:', e.message);
          console.log('Raw response:', data);
        }
      } else {
        console.log(data);
      }
      console.log('\n' + '='.repeat(60));
    });
  });

  req.on('error', (e) => {
    console.error(`❌ Problem with request: ${e.message}`);
  });

  req.end();
}


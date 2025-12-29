#!/usr/bin/env node
/**
 * Test script to verify /api/trips/:id/seats endpoint
 * Shows all seats with isBooked status
 */

const http = require('http');

// Configuration
const BACKEND_HOST = 'localhost';
const BACKEND_PORT = 3000;
const TRIP_ID = 1; // Change this to test different trips

function testGetAllSeats() {
  console.log('\n📋 Test 1: GET /api/trips/:id/seats (All seats with isBooked status)');
  console.log('═'.repeat(60));
  makeRequest(`/api/trips/${TRIP_ID}/seats`);
}

function testGetAvailableSeats() {
  setTimeout(() => {
    console.log('\n\n📋 Test 2: GET /api/trips/:id/seats?available=true (Only available seats)');
    console.log('═'.repeat(60));
    makeRequest(`/api/trips/${TRIP_ID}/seats?available=true`);
  }, 2000);
}

function makeRequest(path) {
  const options = {
    hostname: BACKEND_HOST,
    port: BACKEND_PORT,
    path: path,
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  };

  console.log(`\n🔗 URL: http://${BACKEND_HOST}:${BACKEND_PORT}${path}\n`);

  const req = http.request(options, (res) => {
    console.log(`📊 Status: ${res.statusCode}`);

    let data = '';
    res.on('data', (chunk) => {
      data += chunk;
    });

    res.on('end', () => {
      if (res.statusCode === 200) {
        try {
          const parsed = JSON.parse(data);

          if (Array.isArray(parsed)) {
            console.log(`✅ Returned ${parsed.length} seats\n`);

            // Show booked vs available
            const bookedCount = parsed.filter(s => s.isBooked).length;
            const availableCount = parsed.filter(s => !s.isBooked).length;

            console.log(`Booked: ${bookedCount}, Available: ${availableCount}\n`);

            // Show first 10 seats
            console.log('First 10 seats:');
            parsed.slice(0, 10).forEach(seat => {
              const status = seat.isBooked ? '❌ BOOKED' : '✅ FREE';
              console.log(`  ${seat.label.padEnd(5)} - ${status}`);
            });

            if (parsed.length > 10) {
              console.log(`  ... and ${parsed.length - 10} more seats`);
            }
          } else {
            console.log('❌ Response is not an array');
            console.log(parsed);
          }
        } catch (e) {
          console.error('❌ Failed to parse JSON:', e.message);
          console.log('Raw response:', data);
        }
      } else {
        console.log(`❌ Error: ${res.statusCode}`);
        console.log('Response:', data);
      }
    });
  });

  req.on('error', (e) => {
    console.error(`❌ Connection error: ${e.message}`);
    console.log('Make sure backend is running on localhost:3000');
  });

  req.end();
}

// Run tests
console.log('🚀 Testing /api/trips/:id/seats endpoint');
console.log(`Using Trip ID: ${TRIP_ID}`);
testGetAllSeats();
testGetAvailableSeats();


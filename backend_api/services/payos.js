// backend_api/services/payos.js
const PayOS = require('@payos/node');

console.log('Initializing PayOS with credentials:', {
    clientId: process.env.PAYOS_CLIENT_ID ? 'SET' : 'MISSING',
    apiKey: process.env.PAYOS_API_KEY ? 'SET' : 'MISSING',
    checksumKey: process.env.PAYOS_CHECKSUM_KEY ? 'SET' : 'MISSING'
});

if (!process.env.PAYOS_CLIENT_ID || !process.env.PAYOS_API_KEY || !process.env.PAYOS_CHECKSUM_KEY) {
    console.error('❌ PayOS credentials are missing! Check your environment variables.');
}

const payos = new PayOS({
    clientId: process.env.PAYOS_CLIENT_ID,
    apiKey: process.env.PAYOS_API_KEY,
    checksumKey: process.env.PAYOS_CHECKSUM_KEY
});

console.log('✅ PayOS initialized successfully');

module.exports = payos;


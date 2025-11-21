// backend_api/services/payos.js
const PayOS = require('@payos/node');

console.log('Initializing PayOS with credentials:', {
    clientId: process.env.PAYOS_CLIENT_ID ? 'SET' : 'MISSING',
    apiKey: process.env.PAYOS_API_KEY ? 'SET' : 'MISSING',
    checksumKey: process.env.PAYOS_CHECKSUM_KEY ? 'SET' : 'MISSING',
    partnerCode: process.env.PAYOS_PARTNER_CODE ? 'SET' : 'NOT_SET'
});

if (!process.env.PAYOS_CLIENT_ID || !process.env.PAYOS_API_KEY || !process.env.PAYOS_CHECKSUM_KEY) {
    console.error('❌ PayOS credentials are missing! Check your environment variables.');
}

// PayOS SDK expects positional args: (clientId, apiKey, checksumKey, partnerCode?)
const clientId = process.env.PAYOS_CLIENT_ID;
const apiKey = process.env.PAYOS_API_KEY;
const checksumKey = process.env.PAYOS_CHECKSUM_KEY;
const partnerCode = process.env.PAYOS_PARTNER_CODE || undefined;

let payos;
try {
    if (partnerCode) {
        payos = new PayOS(clientId, apiKey, checksumKey, partnerCode);
    } else {
        payos = new PayOS(clientId, apiKey, checksumKey);
    }
    console.log('✅ PayOS initialized successfully');
} catch (e) {
    console.error('Failed to initialize PayOS SDK:', e && e.message ? e.message : e);
}

module.exports = payos;

#!/usr/bin/env node
/**
 * PayOS Payment Test Script
 * Test PayOS integration endpoints
 */

const axios = require('axios');

// Configuration
const BASE_URL = process.env.API_URL || 'http://localhost:10000';
const API_BASE = `${BASE_URL}/api`;

// Test data
const testData = {
    orderId: Date.now().toString(),
    amount: 150000,
    booking_ids: [1, 2, 3]
};

console.log('🧪 PayOS Integration Test Script\n');
console.log('📍 Base URL:', BASE_URL);
console.log('📦 Test Data:', JSON.stringify(testData, null, 2));
console.log('\n' + '='.repeat(50) + '\n');

/**
 * Test 1: Create PayOS Payment
 */
async function testCreatePayment() {
    console.log('✅ TEST 1: Create PayOS Payment');
    console.log('Endpoint: POST /api/payment/payos/create');

    try {
        const response = await axios.post(`${API_BASE}/payment/payos/create`, testData);

        console.log('✅ Status:', response.status);
        console.log('✅ Response:', JSON.stringify(response.data, null, 2));

        if (response.data.checkoutUrl) {
            console.log('\n🔗 Checkout URL:', response.data.checkoutUrl);
            console.log('👉 Copy và mở URL này trên trình duyệt để test thanh toán');
        }

        return response.data;
    } catch (error) {
        console.error('❌ Error:', error.response?.data || error.message);
        return null;
    }
}

/**
 * Test 2: Verify Payment
 */
async function testVerifyPayment(orderId) {
    console.log('\n✅ TEST 2: Verify Payment');
    console.log('Endpoint: POST /api/payment/payos/verify');

    try {
        const response = await axios.post(`${API_BASE}/payment/payos/verify`, {
            orderId: orderId
        });

        console.log('✅ Status:', response.status);
        console.log('✅ Response:', JSON.stringify(response.data, null, 2));

        return response.data;
    } catch (error) {
        console.error('❌ Error:', error.response?.data || error.message);
        return null;
    }
}

/**
 * Test 3: Check payment_orders table
 */
async function testPaymentOrders() {
    console.log('\n✅ TEST 3: Check Payment Orders Table');
    console.log('Note: This requires database access');

    const { Client } = require('pg');
    const client = new Client({
        connectionString: process.env.DATABASE_URL
    });

    try {
        await client.connect();

        const result = await client.query(
            'SELECT * FROM payment_orders ORDER BY created_at DESC LIMIT 5'
        );

        console.log('✅ Recent Payment Orders:');
        console.table(result.rows);

        await client.end();
    } catch (error) {
        console.error('❌ Error:', error.message);
    }
}

/**
 * Main test runner
 */
async function runTests() {
    console.log('🚀 Starting tests...\n');

    // Test 1: Create payment
    const paymentResult = await testCreatePayment();

    if (!paymentResult) {
        console.error('\n❌ Create payment failed. Aborting tests.');
        process.exit(1);
    }

    console.log('\n⏳ Waiting 2 seconds before verify...');
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Test 2: Verify payment
    await testVerifyPayment(testData.orderId);

    // Test 3: Check database (optional)
    if (process.env.DATABASE_URL) {
        await testPaymentOrders();
    } else {
        console.log('\n⚠️  Skipping database test (DATABASE_URL not set)');
    }

    console.log('\n' + '='.repeat(50));
    console.log('✅ Tests completed!');
    console.log('\n💡 Next steps:');
    console.log('1. Copy the checkout URL and complete payment');
    console.log('2. Check if booking status changed to "confirmed"');
    console.log('3. Verify deep link redirect works');
}

// Run tests
runTests().catch(error => {
    console.error('❌ Fatal error:', error);
    process.exit(1);
});


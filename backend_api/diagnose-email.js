#!/usr/bin/env node
// Diagnostic script for email sending issues

require("dotenv").config();

console.log("\n╔════════════════════════════════════════════════════════╗");
console.log("║     EMAIL SENDING DIAGNOSTIC REPORT                   ║");
console.log("║     Date:", new Date().toLocaleString('vi-VN'), "║");
console.log("╚════════════════════════════════════════════════════════╝\n");

// 1. Environment variables
console.log("1️⃣ ENVIRONMENT VARIABLES");
console.log("───────────────────────────────────────────────────────");
const configs = {
    'TICKET_API_KEY': process.env.TICKET_API_KEY,
    'SENDGRID_API_KEY': process.env.SENDGRID_API_KEY,
    'TICKET_FROM': process.env.TICKET_FROM,
    'SENDGRID_FROM': process.env.SENDGRID_FROM,
};

Object.entries(configs).forEach(([key, value]) => {
    if (value) {
        const masked = value.substring(0, 10) + '...' + value.substring(value.length - 5);
        console.log(`  ✅ ${key}: ${masked}`);
    } else {
        console.log(`  ❌ ${key}: NOT SET`);
    }
});

const apiKey = process.env.TICKET_API_KEY || process.env.SENDGRID_API_KEY;
console.log(`\n  Using API Key: ${process.env.TICKET_API_KEY ? 'TICKET_API_KEY' : 'SENDGRID_API_KEY'}`);

// 2. Module loading
console.log("\n2️⃣ MODULE LOADING");
console.log("───────────────────────────────────────────────────────");

try {
    const SendGridMail = require("@sendgrid/mail");
    console.log("  ✅ @sendgrid/mail loaded");
} catch (e) {
    console.log("  ❌ @sendgrid/mail:", e.message);
}

try {
    const QRCode = require("qrcode");
    console.log("  ✅ qrcode loaded");
} catch (e) {
    console.log("  ❌ qrcode:", e.message);
}

try {
    const sendPaymentEmail = require("./utils/sendPaymentEmail");
    console.log("  ✅ sendPaymentEmail loaded");
    console.log(`     Type: ${typeof sendPaymentEmail}`);
} catch (e) {
    console.log("  ❌ sendPaymentEmail:", e.message);
}

// 3. SendGrid service
console.log("\n3️⃣ SENDGRID SERVICE");
console.log("───────────────────────────────────────────────────────");
try {
    const SendGridMail = require("@sendgrid/mail");
    const sgMail = new SendGridMail.MailService();
    if (apiKey) {
        sgMail.setApiKey(apiKey);
        console.log("  ✅ SendGrid service initialized");
        console.log("  ✅ API key set successfully");
    } else {
        console.log("  ❌ No API key available");
    }
} catch (e) {
    console.log("  ❌ Service initialization:", e.message);
}

// 4. Database
console.log("\n4️⃣ DATABASE CONNECTION");
console.log("───────────────────────────────────────────────────────");
try {
    const db = require("./db");
    console.log("  ✅ Database module loaded");
} catch (e) {
    console.log("  ❌ Database:", e.message);
}

// 5. Email addresses
console.log("\n5️⃣ EMAIL ADDRESSES");
console.log("───────────────────────────────────────────────────────");
const fromEmail = process.env.TICKET_FROM || "dieulien2005@gmail.com";
console.log(`  From: ${fromEmail}`);
console.log("  ⚠️  Make sure this email is VERIFIED on SendGrid!");

// 6. File requirements
console.log("\n6️⃣ FILE INTEGRITY");
console.log("───────────────────────────────────────────────────────");
const fs = require('fs');
const path = require('path');

const files = [
    'utils/sendPaymentEmail.js',
    'controllers/paymentController.js',
    'routes/adminRoutes.js',
    'routes/bookingRoutes.js'
];

files.forEach(file => {
    const filePath = path.join(__dirname, file);
    if (fs.existsSync(filePath)) {
        const content = fs.readFileSync(filePath, 'utf8');
        const hasSendEmail = content.includes('sendPaymentConfirmationEmail');
        console.log(`  ✅ ${file}`);
        if (hasSendEmail) {
            console.log(`     ✅ Contains sendPaymentConfirmationEmail call`);
        } else {
            console.log(`     ⚠️  No sendPaymentConfirmationEmail call found`);
        }
    } else {
        console.log(`  ❌ ${file} not found`);
    }
});

// 7. Summary
console.log("\n7️⃣ SUMMARY");
console.log("───────────────────────────────────────────────────────");
const hasApiKey = !!apiKey;
const fromEmailSet = !!process.env.TICKET_FROM || !!process.env.SENDGRID_FROM;

if (hasApiKey && fromEmailSet) {
    console.log("✅ All basic requirements met!");
    console.log("\nIf email is still not sending:");
    console.log("  1. Check SendGrid account balance");
    console.log("  2. Verify 'from' email address on SendGrid");
    console.log("  3. Check Render Logs for error messages");
    console.log("  4. Test with 'node check-email-config.js'");
} else {
    console.log("❌ Missing critical configuration:");
    if (!hasApiKey) console.log("  - API key not set");
    if (!fromEmailSet) console.log("  - From email not set");
}

console.log("\n╚════════════════════════════════════════════════════════╝\n");


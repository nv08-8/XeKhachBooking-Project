// Debug script to trace email sending issues
require("dotenv").config();

const SendGridMail = require("@sendgrid/mail");

console.log("\n=== DEBUG: Email Sending Configuration ===\n");

// 1. Check environment variables
console.log("1️⃣ Environment Variables:");
console.log("   TICKET_API_KEY:", process.env.TICKET_API_KEY ? "SET ✅" : "NOT SET ❌");
console.log("   SENDGRID_API_KEY:", process.env.SENDGRID_API_KEY ? "SET ✅" : "NOT SET ❌");
console.log("   TICKET_FROM:", process.env.TICKET_FROM || "dieulien2005@gmail.com (default)");
console.log("   SENDGRID_FROM:", process.env.SENDGRID_FROM || "NOT SET");

// 2. Check which API key will be used
const apiKey = process.env.TICKET_API_KEY || process.env.SENDGRID_API_KEY;
console.log("\n2️⃣ API Key Selection:");
console.log("   Using:", process.env.TICKET_API_KEY ? "TICKET_API_KEY" : "SENDGRID_API_KEY");
console.log("   API Key starts with:", apiKey ? apiKey.substring(0, 10) + "..." : "NONE");

// 3. Test SendGrid Mail Service
console.log("\n3️⃣ SendGrid Mail Service Test:");
const sgMail = new SendGridMail.MailService();

try {
    if (apiKey) {
        sgMail.setApiKey(apiKey);
        console.log("   ✅ API Key set successfully");
    } else {
        console.log("   ❌ No API Key available");
    }
} catch (err) {
    console.log("   ❌ Error setting API Key:", err.message);
}

// 4. Check sendPaymentEmail module
console.log("\n4️⃣ Module Check:");
try {
    const sendPaymentEmail = require("./utils/sendPaymentEmail");
    console.log("   ✅ sendPaymentEmail module loaded");
    console.log("   Type:", typeof sendPaymentEmail);
} catch (err) {
    console.log("   ❌ Error loading module:", err.message);
}

console.log("\n=== Possible Issues ===");
console.log("1. API Key not set → Check Render Environment Variables");
console.log("2. Email address not verified → Verify in SendGrid");
console.log("3. Credit exceeded → Check SendGrid account");
console.log("4. Wrong endpoint called → Check app code");
console.log("5. Function not awaited → Check async/await usage");

console.log("\n=== END DEBUG ===\n");


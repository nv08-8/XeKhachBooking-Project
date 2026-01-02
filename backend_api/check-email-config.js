// Test script to verify environment variables and SendGrid setup
require("dotenv").config();

console.log("\n=== EMAIL CONFIGURATION CHECK ===\n");

console.log("✓ TICKET_API_KEY:", process.env.TICKET_API_KEY ? "SET ✅" : "NOT SET ❌");
console.log("✓ SENDGRID_API_KEY:", process.env.SENDGRID_API_KEY ? "SET ✅" : "NOT SET ❌");
console.log("✓ TICKET_FROM:", process.env.TICKET_FROM || "NOT SET (will use default: dieulien2005@gmail.com)");
console.log("✓ SENDGRID_FROM:", process.env.SENDGRID_FROM || "NOT SET");

const apiKey = process.env.TICKET_API_KEY || process.env.SENDGRID_API_KEY;
if (!apiKey) {
    console.error("\n❌ NO API KEY FOUND! Email sending will fail.");
    console.error("   Make sure to set either TICKET_API_KEY or SENDGRID_API_KEY in your environment.");
} else {
    console.log("\n✅ API Key is available for email sending");
    console.log("   Using:", process.env.TICKET_API_KEY ? "TICKET_API_KEY" : "SENDGRID_API_KEY");
}

// Try to import sendPaymentEmail module
try {
    const sendPaymentEmail = require("./utils/sendPaymentEmail");
    console.log("✅ sendPaymentEmail module loaded successfully");
} catch (err) {
    console.error("❌ Failed to load sendPaymentEmail module:", err.message);
}

console.log("\n=== END CHECK ===\n");


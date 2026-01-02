const sgMailLib = require("@sendgrid/mail");

// Create a separate instance for OTP emails with SENDGRID_API_KEY
const sgMail = sgMailLib;
sgMail.setApiKey(process.env.SENDGRID_API_KEY);

async function sendEmail(to, subject, text) {
    try {
        // Ensure API key is set before sending (prevents override by other modules)
        if (!process.env.SENDGRID_API_KEY) {
            throw new Error("SENDGRID_API_KEY is not set in environment variables");
        }
        sgMail.setApiKey(process.env.SENDGRID_API_KEY);

        const msg = {
            to,
            from: {
                email: process.env.SENDGRID_FROM,
                name: "GoUTE Ticket System"  // thêm tên sẽ tăng uy tín email
            },
            subject,
            text,
            html: `<p>${text}</p>`,     // có HTML sẽ ít bị đánh spam hơn
        };

        const response = await sgMail.send(msg);
        console.log("📧 Email sent!", response[0].statusCode);
    } catch (error) {
        console.error("❌ SendGrid Error:", error.response?.body || error);
    }
}

module.exports = sendEmail;

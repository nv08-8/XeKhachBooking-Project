const sgMail = require("@sendgrid/mail");

sgMail.setApiKey(process.env.SENDGRID_API_KEY);

async function sendEmail(to, subject, text) {
    try {
        const msg = {
            to,
            from: process.env.SENDGRID_FROM,  // email gửi đi
            subject,
            text,
        };

        const response = await sgMail.send(msg);
        console.log("📧 SendGrid email sent!", response[0].statusCode);
    } catch (error) {
        console.error("❌ SendGrid email error:", error.response?.body || error);
    }
}

module.exports = sendEmail;
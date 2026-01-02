const SendGridMail = require("@sendgrid/mail");

// Create a COMPLETELY SEPARATE instance for OTP emails
// This prevents any possibility of API key conflict with payment emails
class SendGridOTP {
    constructor() {
        this.client = new SendGridMail.MailService();
        if (process.env.SENDGRID_API_KEY) {
            this.client.setApiKey(process.env.SENDGRID_API_KEY);
        }
    }

    async send(msg) {
        return this.client.send(msg);
    }
}

const sgMail = new SendGridOTP();

async function sendEmail(to, subject, text) {
    try {
        // Ensure API key is set before sending
        if (!process.env.SENDGRID_API_KEY) {
            throw new Error("SENDGRID_API_KEY is not set in environment variables");
        }

        const msg = {
            to,
            from: {
                email: process.env.SENDGRID_FROM,
                name: "GoUTE Ticket System"
            },
            subject,
            text,
            html: `<p>${text}</p>`,
        };

        const response = await sgMail.send(msg);
        console.log("📧 OTP Email sent to", to, "- Status:", response[0].statusCode);
    } catch (error) {
        console.error("❌ SendGrid OTP Error:", error.response?.body || error);
    }
}

module.exports = sendEmail;

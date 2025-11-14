const { Resend } = require("resend");
const resend = new Resend(process.env.RESEND_API_KEY);

async function sendEmail(to, subject, text) {
    try {
        const data = await resend.emails.send({
            from: "GoUTE <onboarding@resend.dev>",
            to: to,
            subject: subject,
            text: text,
        });

        console.log("📧 Email sent:", data);
    } catch (error) {
        console.error("❌ Send email failed:", error);
    }
}

module.exports = sendEmail;

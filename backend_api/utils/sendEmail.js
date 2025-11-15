const sgMail = require("@sendgrid/mail");
sgMail.setApiKey(process.env.SENDGRID_API_KEY);

async function sendEmail(to, otp) {
    try {
        const msg = {
            to,
            from: process.env.SENDGRID_FROM,
            templateId: process.env.SENDGRID_TEMPLATE_ID,
            dynamic_template_data: {
                otp
            }
        };

        await sgMail.send(msg);
        console.log("📧 OTP sent!");
    } catch (error) {
        console.error("❌ SendGrid Error:", error.response?.body || error);
    }
}

module.exports = sendEmail;

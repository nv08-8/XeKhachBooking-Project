const nodemailer = require("nodemailer");

async function sendEmail(to, subject, text) {
    const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
            user: process.env.MAIL_USER,
            pass: process.env.MAIL_PASS,
        },
    });

    await transporter.sendMail({
        from: `"GoUTE" <${process.env.MAIL_USER}>`,
        to,
        subject,
        text,
    });
}

module.exports = sendEmail;

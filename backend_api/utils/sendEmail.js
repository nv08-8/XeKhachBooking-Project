const nodemailer = require("nodemailer");

async function sendEmail(to, subject, text) {
    const transporter = nodemailer.createTransport({
        host: "smtp.gmail.com",
        port: 587,
        secure: false, // TLS
        auth: {
            user: process.env.MAIL_USER,
            pass: process.env.MAIL_PASS,
        },
        tls: {
            rejectUnauthorized: false,
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

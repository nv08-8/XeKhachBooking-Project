const SendGridMail = require("@sendgrid/mail");
const QRCode = require('qrcode');
const fs = require('fs');
const path = require('path');

// Create a COMPLETELY SEPARATE instance for payment emails
// This prevents any possibility of API key conflict with OTP emails
class SendGridPayment {
    constructor() {
        this.client = new SendGridMail.MailService();
        const apiKey = process.env.TICKET_API_KEY || process.env.SENDGRID_API_KEY;
        if (apiKey) {
            this.client.setApiKey(apiKey);
        }
    }

    async send(msg) {
        return this.client.send(msg);
    }
}

let sgMail;
try {
    sgMail = new SendGridPayment();
    console.log("[sendPaymentEmail] ✅ SendGrid Mail service initialized");
} catch (initError) {
    console.error("[sendPaymentEmail] ❌ Failed to initialize SendGrid:", initError.message);
    sgMail = null;
}

/**
 * Format date to Vietnam timezone (UTC+7)
 * Assumes the input date is stored as UTC in database
 * @param {Date|string} date - Date to format (UTC from database)
 * @param {boolean} includeTime - Whether to include time
 * @returns {string} Formatted date string in Vietnam timezone (UTC+7)
 */
function formatDateInVietnamTZ(date, includeTime = false) {
    try {
        const d = new Date(date);

        // Convert UTC to UTC+7 by adding 7 hours
        // The Date object stores time in UTC internally
        const vietnamTime = new Date(d.getTime() + (7 * 60 * 60 * 1000));

        // Format without timezone offset conversion (use UTC components directly)
        const year = vietnamTime.getUTCFullYear();
        const month = String(vietnamTime.getUTCMonth() + 1).padStart(2, '0');
        const day = String(vietnamTime.getUTCDate()).padStart(2, '0');

        if (includeTime) {
            const hours = String(vietnamTime.getUTCHours()).padStart(2, '0');
            const minutes = String(vietnamTime.getUTCMinutes()).padStart(2, '0');
            const seconds = String(vietnamTime.getUTCSeconds()).padStart(2, '0');
            return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`;
        } else {
            return `${day}/${month}/${year}`;
        }
    } catch (err) {
        console.warn("⚠️ Failed to format date:", err.message);
        return new Date().toLocaleDateString('vi-VN');
    }
}

/**
 * Format paid_at timestamp (already stored as UTC+7 in database)
 * NO NEED to add 7 hours - the timestamp is already in Asia/Ho_Chi_Minh timezone
 * @param {Date|string} date - Payment time from paid_at column
 * @returns {string} Formatted time string (dd/MM/yyyy HH:mm:ss)
 */
function formatPaidAtTime(date) {
    try {
        if (!date) return 'N/A';
        const d = new Date(date);

        // Format directly without adding 7 hours (paid_at is already UTC+7)
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');

        return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`;
    } catch (err) {
        console.warn("⚠️ Failed to format paid_at time:", err.message);
        return 'N/A';
    }
}

/**
 * Send payment confirmation email with booking and ticket details
 * @param {string} email - Customer email
 * @param {object} booking - Booking data from database
 * @param {object} trip - Trip data from database
 * @param {object} user - User data from database
 */
async function sendPaymentConfirmationEmail(email, booking, trip, user) {
    try {
        if (!email) {
            console.error("❌ Email address is required");
            return false;
        }

        // Check if SendGrid service is initialized
        if (!sgMail) {
            console.error("❌ SendGrid mail service not initialized");
            return false;
        }

        // Verify API key is available
        const apiKey = process.env.TICKET_API_KEY || process.env.SENDGRID_API_KEY;
        if (!apiKey) {
            console.error("❌ Neither TICKET_API_KEY nor SENDGRID_API_KEY is set");
            console.error("   - TICKET_API_KEY:", process.env.TICKET_API_KEY ? "SET" : "NOT SET");
            console.error("   - SENDGRID_API_KEY:", process.env.SENDGRID_API_KEY ? "SET" : "NOT SET");
            return false;
        }

        console.log(`[sendPaymentEmail] Starting email send for booking ${booking.id}, email: ${email}`);

        // Format dates and prices
        const departureDate = new Date(trip.departure_time).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        const pricePaid = booking.price_paid || booking.total_amount || 0;
        const formattedPrice = new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(pricePaid);

        // Generate QR code as buffer for attachment
        const bookingCode = booking.booking_code || '#' + booking.id;
        let qrCodeBuffer = null;
        try {
            qrCodeBuffer = await QRCode.toBuffer(bookingCode, {
                errorCorrectionLevel: 'H',
                type: 'image/png',
                quality: 0.92,
                margin: 1,
                width: 200,
                color: {
                    dark: '#000000',
                    light: '#FFFFFF'
                }
            });
            console.log("✅ QR code generated as attachment");
        } catch (qrError) {
            console.warn("⚠️ Failed to generate QR code:", qrError.message);
        }

        // Generate HTML email template
        const htmlContent = `
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #3498db 0%, #2980b9 100%); color: white; padding: 30px 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .logo { height: 60px; margin-bottom: 15px; }
                    .header h1 { margin: 10px 0; font-size: 28px; }
                    .content { background-color: #f9f9f9; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .section { margin: 15px 0; }
                    .label { font-weight: bold; color: #3498db; }
                    .divider { border-top: 2px solid #e0e0e0; margin: 20px 0; }
                    .footer { font-size: 12px; color: #999; text-align: center; margin-top: 30px; }
                    .booking-code {
                        background-color: #3498db;
                        color: white;
                        padding: 15px;
                        border-radius: 5px;
                        font-size: 20px;
                        font-weight: bold;
                        text-align: center;
                        margin: 15px 0;
                    }
                    .qr-section {
                        text-align: center;
                        padding: 20px;
                        background-color: white;
                        border: 2px dashed #3498db;
                        border-radius: 5px;
                        margin: 20px 0;
                    }
                    .qr-section img {
                        max-width: 220px;
                        height: auto;
                        margin: 10px 0;
                    }
                    .qr-label {
                        font-size: 14px;
                        color: #666;
                        margin-top: 10px;
                        font-weight: bold;
                    }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f2f2f2; font-weight: bold; color: #3498db; }
                    .note-box {
                        background-color: #e8f4f8;
                        border-left: 4px solid #3498db;
                        padding: 15px;
                        border-radius: 5px;
                        margin: 15px 0;
                    }
                    .note-box ul {
                        margin: 10px 0;
                        padding-left: 20px;
                    }
                    .note-box li {
                        margin: 8px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Thanh toán vé thành công!</h1>
                        <p style="margin: 0; font-size: 14px; opacity: 0.9;">XeKhachBooking - Đặt vé xe khách online</p>
                    </div>

                    <div class="content">
                        <div class="section">
                            <p>Xin chào <span class="label">${user.name || 'Khách hàng'}</span>,</p>
                            <p>Cảm ơn bạn đã đặt vé tại XeKhachBooking. Đơn đặt vé của bạn đã được xác nhận và thanh toán thành công.</p>
                        </div>

                        <div class="divider"></div>

                        <div class="booking-code">
                            🎫 ${bookingCode}
                        </div>

                        <div class="qr-section">
                            <p style="margin: 0 0 10px 0; font-weight: bold;">📱 QR Code vé của bạn</p>
                            <p style="color: #666; font-size: 13px; margin: 10px 0;">QR code vé được gửi kèm trong tệp đính kèm</p>
                            <div class="qr-label">Quét mã QR để xác nhận vé tại điểm lên xe</div>
                        </div>

                        <div class="divider"></div>

                        <div class="section">
                            <h3>📋 Thông tin đặt vé</h3>
                            <table>
                                <tr>
                                    <th>Chi tiết</th>
                                    <th>Nội dung</th>
                                </tr>
                                <tr>
                                    <td>Mã đặt vé</td>
                                    <td><strong>${bookingCode}</strong></td>
                                </tr>
                                <tr>
                                    <td>Trạng thái</td>
                                    <td>✅ Đã xác nhận</td>
                                </tr>
                                <tr>
                                    <td>Ngày đặt</td>
                                    <td>${new Date(booking.created_at).toLocaleDateString('vi-VN')}</td>
                                </tr>
                            </table>
                        </div>

                        <div class="divider"></div>

                        <div class="section">
                            <h3>🚌 Thông tin chuyến xe</h3>
                            <table>
                                <tr>
                                    <th>Chi tiết</th>
                                    <th>Nội dung</th>
                                </tr>
                                <tr>
                                    <td>Tuyến đường</td>
                                    <td><strong>${trip.origin} → ${trip.destination}</strong></td>
                                </tr>
                                <tr>
                                    <td>Ngày khởi hành</td>
                                    <td>${departureDate}</td>
                                </tr>
                                <tr>
                                    <td>Hãng xe</td>
                                    <td>${trip.operator || 'XeKhach'}</td>
                                </tr>
                                <tr>
                                    <td>Loại xe</td>
                                    <td>${trip.bus_type || 'Giường nằm'}</td>
                                </tr>
                                <tr>
                                    <td>Số ghế</td>
                                    <td><strong>${Array.isArray(booking.seat_labels) ? booking.seat_labels.join(', ') : (booking.seat_codes || 'N/A')}</strong></td>
                                </tr>
                            </table>
                        </div>

                        <div class="divider"></div>

                        <div class="section">
                            <h3>💳 Thông tin thanh toán</h3>
                            <table>
                                <tr>
                                    <th>Chi tiết</th>
                                    <th>Nội dung</th>
                                </tr>
                                <tr>
                                    <td>Tổng tiền</td>
                                    <td><strong style="color: #3498db; font-size: 16px;">${formattedPrice}</strong></td>
                                </tr>
                                <tr>
                                    <td>Phương thức thanh toán</td>
                                    <td>${booking.payment_method || 'Chuyển khoản'}</td>
                                </tr>
                                <tr>
                                    <td>Thời gian thanh toán</td>
                                    <td>${formatPaidAtTime(booking.paid_at)}</td>
                                </tr>
                            </table>
                        </div>

                        <div class="divider"></div>

                        <div class="section">
                            <h3>👤 Thông tin hành khách</h3>
                            <table>
                                <tr>
                                    <th>Chi tiết</th>
                                    <th>Nội dung</th>
                                </tr>
                                <tr>
                                    <td>Tên hành khách</td>
                                    <td>${user.name || 'Chưa cập nhật'}</td>
                                </tr>
                                <tr>
                                    <td>Số điện thoại</td>
                                    <td>${user.phone || 'Chưa cập nhật'}</td>
                                </tr>
                                <tr>
                                    <td>Email</td>
                                    <td>${user.email}</td>
                                </tr>
                            </table>
                        </div>

                        <div class="note-box">
                            <p style="margin: 0 0 10px 0;"><strong>⚠️ Lưu ý quan trọng:</strong></p>
                            <ul style="margin: 0;">
                                <li>✅ Vui lòng đến điểm đón xe <strong>15 phút trước</strong> giờ khởi hành</li>
                                <li>✅ Mang theo <strong>ID xác thực</strong> (CMND/Passport) phù hợp</li>
                                <li>✅ Liên hệ hotline nếu có bất kỳ thay đổi</li>
                                <li>✅ Lưu lại QR code để quét tại điểm lên xe</li>
                            </ul>
                        </div>
                    </div>

                    <div class="footer">
                        <p style="margin: 10px 0;">📞 Liên hệ: support@xekhachbooking.com</p>
                        <p style="margin: 10px 0;">Email này được gửi tự động. Vui lòng không trả lời email này.</p>
                        <p style="margin: 10px 0;">&copy; 2026 XeKhachBooking. Tất cả quyền được bảo lưu.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        const msg = {
            to: email,
            from: {
                email: process.env.TICKET_FROM || "dieulien2005@gmail.com",
                name: "XeKhachBooking"
            },
            subject: `Xác nhận thanh toán vé ${booking.booking_code || '#' + booking.id}`,
            html: htmlContent,
            attachments: []
        };

        // Add QR code as attachment
        if (qrCodeBuffer) {
            msg.attachments.push({
                content: qrCodeBuffer.toString('base64'),
                filename: `qr-code-${bookingCode}.png`,
                type: 'image/png',
                disposition: 'attachment'
            });
            console.log("✅ QR code added as attachment");
        }

        console.log(`[sendPaymentEmail] Preparing to send email to: ${email}`);
        console.log(`[sendPaymentEmail] From: ${msg.from.email}`);
        console.log(`[sendPaymentEmail] Subject: ${msg.subject}`);
        console.log(`[sendPaymentEmail] Attachments: ${msg.attachments ? msg.attachments.length : 0}`);

        // Debug seat codes
        console.log(`[sendPaymentEmail] Seat codes: ${booking.seat_codes || 'N/A'}`);

        const response = await sgMail.send(msg);
        console.log("📧 Payment confirmation email sent to", email, "- Status:", response[0].statusCode);
        console.log(`[sendPaymentEmail] Success! Response status: ${response[0].statusCode}`);
        return {
            success: true,
            email: email,
            status: response[0].statusCode,
            message: "Email sent successfully"
        };
    } catch (error) {
        console.error("❌ Failed to send payment confirmation email:", error.response?.body || error.message || error);
        if (error.response?.body?.errors) {
            console.error("   SendGrid Errors:", error.response.body.errors);
        }
        return {
            success: false,
            email: email,
            error: error.message || "Unknown error",
            details: error.response?.body || error
        };
    }
}

module.exports = sendPaymentConfirmationEmail;

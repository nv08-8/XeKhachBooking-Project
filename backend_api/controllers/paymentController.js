// backend_api/controllers/paymentController.js
const payos = require('../services/payos');
const db = require('../db');

// Create PayOS checkout link
exports.createCheckout = async (req, res) => {
    const { orderId, amount, booking_ids } = req.body;

    console.log('createCheckout called with:', { orderId, amount, booking_ids });

    if (!orderId || !amount) {
        return res.status(400).json({ error: 'Missing orderId or amount' });
    }

    // Verify PayOS credentials
    if (!process.env.PAYOS_CLIENT_ID || !process.env.PAYOS_API_KEY || !process.env.PAYOS_CHECKSUM_KEY) {
        console.error('PayOS credentials missing!');
        return res.status(500).json({ error: 'PayOS not configured. Please contact administrator.' });
    }

    try {
        const paymentData = {
            orderCode: Number(orderId),
            amount: Number(amount),
            description: 'Thanh toán vé xe khách',
            returnUrl: process.env.PAYMENT_RETURN_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/return',
            cancelUrl: process.env.PAYMENT_CANCEL_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/cancel'
        };

        console.log('Creating PayOS payment link with:', paymentData);

        const payment = await payos.createPaymentLink(paymentData);

        console.log('PayOS response:', payment);

        // Persist order mapping to booking_ids
        if (Array.isArray(booking_ids) && booking_ids.length) {
            try {
                await db.query(
                    'INSERT INTO payment_orders(order_code, booking_ids, amount, created_at) VALUES($1, $2, $3, NOW())',
                    [orderId, JSON.stringify(booking_ids), amount]
                );
                console.log('Payment order saved to DB');
            } catch (e) {
                console.warn('Could not persist payment order mapping:', e.message || e);
            }
        }

        res.json({ checkoutUrl: payment.checkoutUrl });
    } catch (err) {
        console.error('createCheckout error:', err);
        console.error('Error stack:', err.stack);
        res.status(500).json({
            error: err.message || 'Failed to create checkout',
            details: process.env.NODE_ENV === 'development' ? err.stack : undefined
        });
    }
};

// Handle return redirect from PayOS
exports.handleReturn = async (req, res) => {
    const params = req.query || {};
    console.log('PayOS return params:', params);

    // Extract orderCode and status from PayOS callback
    const orderCode = params.orderCode || params.code || params.order_id || params.orderId;
    const status = params.status || params.resultCode;
    const transId = params.id || params.transactionId || params.transId;

    // Check if payment is successful
    const success = status && (String(status).toLowerCase() === 'paid' || String(status) === '00' || String(status).toLowerCase() === 'success');

    if (orderCode && success) {
        try {
            const { rows } = await db.query('SELECT booking_ids FROM payment_orders WHERE order_code=$1 ORDER BY created_at DESC LIMIT 1', [orderCode]);
            if (rows && rows.length) {
                const bookingIds = JSON.parse(rows[0].booking_ids);
                for (const id of bookingIds) {
                    await db.query("UPDATE bookings SET status='confirmed', payment_method='payos', payment_time=NOW() WHERE id=$1", [id]);
                }
            }
        } catch (e) {
            console.warn('Could not auto-confirm bookings on return:', e.message || e);
        }
    }

    // Respond with HTML that redirects to app via deep link
    const html = `
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Thanh toán</title>
            <style>
                body {
                    font-family: 'Segoe UI', Arial, sans-serif;
                    padding: 40px 20px;
                    text-align: center;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .container {
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    max-width: 400px;
                }
                .icon {
                    font-size: 60px;
                    margin-bottom: 20px;
                }
                h2 {
                    color: #333;
                    margin-bottom: 10px;
                }
                .info {
                    color: #666;
                    margin: 10px 0;
                    font-size: 14px;
                }
                .btn {
                    display: inline-block;
                    margin-top: 20px;
                    padding: 12px 30px;
                    background: #667eea;
                    color: white;
                    text-decoration: none;
                    border-radius: 25px;
                    font-weight: bold;
                }
            </style>
          </head>
          <body>
            <div class="container">
                <div class="icon">${success ? '✅' : '❌'}</div>
                <h2>Thanh toán ${success ? 'thành công!' : 'không thành công'}</h2>
                <div class="info">Mã đơn: <strong>${orderCode || 'N/A'}</strong></div>
                <div class="info">Mã giao dịch: <strong>${transId || 'N/A'}</strong></div>
                <p style="margin-top: 20px; color: #888;">Đang chuyển về ứng dụng...</p>
                <a id="backLink" href="xekhachbooking://payment?order=${orderCode}&status=${success ? 'success' : 'failed'}&transactionId=${transId || ''}" class="btn">
                    Mở App
                </a>
            </div>
            <script>
                setTimeout(() => {
                    window.location.href = 'xekhachbooking://payment?order=${orderCode}&status=${success ? 'success' : 'failed'}&transactionId=${transId || ''}';
                }, 2000);
            </script>
          </body>
        </html>
    `;

    res.set('Content-Type', 'text/html; charset=utf-8');
    res.send(html);
};

// Handle cancel redirect
exports.handleCancel = (req, res) => {
    const params = req.query || {};
    console.log('PayOS cancel params:', params);

    const orderCode = params.orderCode || params.code || params.orderId;

    const html = `
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Hủy thanh toán</title>
            <style>
                body {
                    font-family: 'Segoe UI', Arial, sans-serif;
                    padding: 40px 20px;
                    text-align: center;
                    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .container {
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    max-width: 400px;
                }
                .icon { font-size: 60px; margin-bottom: 20px; }
                h2 { color: #333; margin-bottom: 10px; }
                .btn {
                    display: inline-block;
                    margin-top: 20px;
                    padding: 12px 30px;
                    background: #f5576c;
                    color: white;
                    text-decoration: none;
                    border-radius: 25px;
                    font-weight: bold;
                }
            </style>
          </head>
          <body>
            <div class="container">
                <div class="icon">⚠️</div>
                <h2>Đã hủy thanh toán</h2>
                <p style="color: #666;">Bạn đã hủy giao dịch</p>
                <p style="margin-top: 20px; color: #888;">Đang chuyển về ứng dụng...</p>
                <a href="xekhachbooking://payment?order=${orderCode || ''}&status=cancelled" class="btn">
                    Mở App
                </a>
            </div>
            <script>
                setTimeout(() => {
                    window.location.href = 'xekhachbooking://payment?order=${orderCode || ''}&status=cancelled';
                }, 2000);
            </script>
          </body>
        </html>
    `;

    res.set('Content-Type', 'text/html; charset=utf-8');
    res.send(html);
};

// Verify payment (called by app after deep link or manually)
exports.verifyPayment = async (req, res) => {
    const { orderId, transactionId } = req.body || req.query || {};

    if (!orderId && !transactionId) {
        return res.status(400).json({ error: 'Missing orderId or transactionId' });
    }

    try {
        // Attempt to confirm via PayOS SDK if available
        let payosStatus = null;
        try {
            if (transactionId && typeof payos.getPayment === 'function') {
                payosStatus = await payos.getPayment({ transactionId });
            } else if (orderId && typeof payos.getPaymentByOrder === 'function') {
                payosStatus = await payos.getPaymentByOrder({ orderCode: orderId });
            }
        } catch (e) {
            console.warn('PayOS SDK verify not available or failed:', e.message || e);
            payosStatus = null;
        }

        // Resolve booking IDs from payment_orders table if present
        let bookingIds = [];
        try {
            const key = orderId || transactionId;
            const { rows } = await db.query('SELECT booking_ids FROM payment_orders WHERE order_code=$1 ORDER BY created_at DESC LIMIT 1', [key]);
            if (rows && rows.length) {
                bookingIds = JSON.parse(rows[0].booking_ids);
            }
        } catch (e) {
            console.warn('Could not read payment_orders mapping:', e.message || e);
        }

        // If PayOS reports success or we have transactionId presence, mark bookings as confirmed
        const successFromPayos = payosStatus && (payosStatus.status === 'SUCCESS' || String(payosStatus.status).toLowerCase() === 'success' || payosStatus.code === '00');
        const considerSuccess = successFromPayos || !!transactionId;

        if (considerSuccess && bookingIds.length) {
            for (const id of bookingIds) {
                await db.query("UPDATE bookings SET status='confirmed', payment_method='payos', payment_time=NOW() WHERE id=$1", [id]);
            }
        }

        return res.json({ ok: true, booking_ids: bookingIds, payosStatus });
    } catch (err) {
        console.error('verifyPayment error:', err);
        return res.status(500).json({ error: err.message || 'Verify failed' });
    }
};

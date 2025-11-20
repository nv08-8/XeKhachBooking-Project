// backend_api/controllers/paymentController.js
const payos = require('../services/payos');
const db = require('../db');

// Create PayOS checkout link
exports.createCheckout = async (req, res) => {
    const { orderId, amount, booking_ids } = req.body;

    if (!orderId || !amount) {
        return res.status(400).json({ error: 'Missing orderId or amount' });
    }

    try {
        const payment = await payos.createPaymentLink({
            orderCode: orderId,
            amount: amount,
            description: 'Thanh toán vé xe',
            returnUrl: process.env.PAYMENT_RETURN_URL || 'https://xekhachbooking-project.onrender.com/payment/return',
            cancelUrl: process.env.PAYMENT_CANCEL_URL || 'https://xekhachbooking-project.onrender.com/payment/cancel'
        });

        // Optionally persist order mapping to booking_ids (for later verification)
        if (Array.isArray(booking_ids) && booking_ids.length) {
            // store a simple mapping in a payment_orders table if exists; otherwise log
            try {
                await db.query(
                    'INSERT INTO payment_orders(order_code, booking_ids, amount, created_at) VALUES($1, $2, $3, NOW())',
                    [orderId, JSON.stringify(booking_ids), amount]
                );
            } catch (e) {
                // If table doesn't exist, just log and continue
                console.warn('Could not persist payment order mapping:', e.message || e);
            }
        }

        res.json({ checkoutUrl: payment.checkoutUrl });
    } catch (err) {
        console.error('createCheckout error:', err);
        res.status(500).json({ error: err.message || 'Failed to create checkout' });
    }
};

// Handle return redirect from PayOS
exports.handleReturn = async (req, res) => {
    // PayOS will redirect users to returnUrl with query params. We log and show a simple page.
    const params = req.query || {};
    console.log('PayOS return params:', params);

    // Try to reconcile payment where possible: if orderCode present, mark bookings as confirmed
    const orderCode = params.orderCode || params.orderCode || params.order_id || params.orderId || params.order;
    const status = params.status || params.result || params.responseCode;
    const transId = params.transactionId || params.transId || params.transaction_id;

    // Basic heuristic: if status indicates success ("success" or "00" or "SUCCESS"), update booking rows
    const success = status && (String(status).toLowerCase() === 'success' || String(status) === '00' || String(status).toUpperCase() === 'SUCCESS');
    if (orderCode && success) {
        try {
            // try to find mapping in payment_orders
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

    // Respond with a small HTML that can redirect user back to app or show success
    const html = `
        <html>
          <head>
            <meta charset="utf-8" />
            <title>Thanh toán</title>
            <style>body{font-family:Arial,sans-serif;padding:24px;text-align:center}</style>
          </head>
          <body>
            <h2>Thanh toán ${success ? 'thành công' : 'không thành công'}</h2>
            <p>Order: ${orderCode || 'n/a'}</p>
            <p>Transaction: ${transId || 'n/a'}</p>
            <p>Bấm <a id="back" href="xekhachbooking://payment?order=${orderCode}">về app</a> để xem vé, hoặc đóng cửa sổ này.</p>
            <script>setTimeout(()=>{document.getElementById('back').click()},3000)</script>
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
    res.redirect(process.env.PAYMENT_CANCEL_PAGE || '/');
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

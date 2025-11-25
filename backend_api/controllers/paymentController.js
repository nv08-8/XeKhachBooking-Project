// backend_api/controllers/paymentController.js
const payos = require('../services/payos');
const db = require('../db');

// Create PayOS checkout link
exports.createCheckout = async (req, res) => {
    const orderId = req.body.orderId || req.body.order_id;
    const amount = req.body.amount;
    const booking_ids = req.body.booking_ids;

    console.log('createCheckout called with:', { orderId, amount, booking_ids });

    if (!orderId || !amount || !Array.isArray(booking_ids) || booking_ids.length === 0) {
        return res.status(400).json({ error: 'Missing orderId, amount, or booking_ids' });
    }

    if (!payos || typeof payos.createPaymentLink !== 'function') {
        console.error('PayOS SDK not initialized properly.');
        return res.status(500).json({ error: 'PayOS SDK not available. Check server logs.' });
    }

    try {
        const { buyerName, buyerEmail, buyerPhone, buyerAddress, items: clientItems } = req.body || {};
        const quantity = booking_ids.length;
        const unitPrice = Math.floor(Number(amount) / quantity) || Number(amount);

        const returnUrl = process.env.PAYMENT_RETURN_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/return';
        const cancelUrl = process.env.PAYMENT_CANCEL_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/cancel';

        const items = Array.isArray(clientItems) && clientItems.length > 0
            ? clientItems
            : [{ name: 'Vé xe khách', quantity: quantity, price: unitPrice }];

        let cleanPhone = '0123456789';
        if (buyerPhone && String(buyerPhone).trim()) {
            cleanPhone = String(buyerPhone).trim().replace(/\s+/g, '');
            if (cleanPhone.startsWith('+84')) cleanPhone = '0' + cleanPhone.substring(3);
            else if (cleanPhone.startsWith('84')) cleanPhone = '0' + cleanPhone.substring(2);
        }

        const paymentData = {
            orderCode: Number(orderId),
            amount: Number(amount),
            description: 'Thanh toan ve xe khach',
            buyerName: (buyerName && String(buyerName).trim()) || 'Khach hang',
            buyerEmail: (buyerEmail && String(buyerEmail).trim()) || 'customer@xekhachbooking.com',
            buyerPhone: cleanPhone,
            buyerAddress: (buyerAddress && String(buyerAddress).trim()) || 'Viet Nam',
            items: items,
            expiredAt: Math.floor(Date.now() / 1000) + (15 * 60),
            returnUrl: returnUrl,
            cancelUrl: cancelUrl
        };

        console.log('Creating PayOS payment link with:', paymentData);

        const payment = await payos.createPaymentLink(paymentData);

        // Store the orderCode in the metadata of each booking
        const paymentInfo = { payment: { orderCode: orderId, provider: 'payos' } };
        for (const bookingId of booking_ids) {
            await db.query(
                "UPDATE bookings SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb WHERE id = $2",
                [JSON.stringify(paymentInfo), bookingId]
            );
        }
        console.log(`Updated ${booking_ids.length} bookings with orderCode ${orderId}`);

        res.json({ checkoutUrl: payment.checkoutUrl });
    } catch (err) {
        console.error('createCheckout error:', err);
        res.status(500).json({ error: err.message || 'Failed to create checkout' });
    }
};

// Handle return redirect from PayOS
exports.handleReturn = async (req, res) => {
    const params = req.query || {};
    console.log('PayOS return params:', params);

    const orderCode = params.orderCode;
    const status = params.status;
    const transId = params.id;
    const success = status && String(status).toLowerCase() === 'paid';

    const deepLink = `xekhachbooking://payment?order=${orderCode}&status=${success ? 'success' : 'failed'}&transactionId=${transId || ''}`;

    const html = `
        <!DOCTYPE html><html><head><title>Thanh toán</title></head>
        <body>
            <h1>Thanh toán ${success ? 'thành công' : 'thất bại'}</h1>
            <p>Đang chuyển về ứng dụng...</p>
            <a href="${deepLink}">Mở App</a>
            <script>setTimeout(() => { window.location.href = '${deepLink}'; }, 2000);</script>
        </body></html>`;

    res.set('Content-Type', 'text/html; charset=utf-8').send(html);
};

// Handle cancel redirect
exports.handleCancel = (req, res) => {
    const orderCode = req.query.orderCode;
    const deepLink = `xekhachbooking://payment?order=${orderCode || ''}&status=cancelled`;
    const html = `
        <!DOCTYPE html><html><head><title>Hủy thanh toán</title></head>
        <body>
            <h1>Đã hủy thanh toán</h1>
            <p>Đang chuyển về ứng dụng...</p>
            <a href="${deepLink}">Mở App</a>
            <script>setTimeout(() => { window.location.href = '${deepLink}'; }, 2000);</script>
        </body></html>`;
    res.set('Content-Type', 'text/html; charset=utf-8').send(html);
};

// Verify payment (called by app)
exports.verifyPayment = async (req, res) => {
    const { orderId } = req.body || {};
    if (!orderId) return res.status(400).json({ error: 'Missing orderId' });

    try {
        const payosStatus = await payos.getPaymentLinkInformation(orderId);
        const isSuccess = payosStatus && payosStatus.status === 'PAID';

        if (isSuccess) {
            const { rows: bookings } = await db.query(
                "SELECT id FROM bookings WHERE metadata->'payment'->>'orderCode' = $1 AND status = 'pending'",
                [String(orderId)]
            );

            if (bookings.length > 0) {
                const bookingIds = bookings.map(b => b.id);
                await db.query(
                    "UPDATE bookings SET status='confirmed', price_paid = total_amount WHERE id = ANY($1::bigint[])",
                    [bookingIds]
                );
                console.log(`Verified and updated bookings: ${bookingIds.join(', ')}`);
                return res.json({ success: true, bookings_updated: bookingIds });
            }
        }
        res.json({ success: isSuccess, payosStatus });
    } catch (err) {
        console.error('verifyPayment error:', err);
        res.status(500).json({ error: err.message || 'Verify failed' });
    }
};

// Webhook handler from PayOS
exports.handleWebhook = async (req, res) => {
    console.log('PayOS webhook received:', JSON.stringify(req.body, null, 2));
    try {
        const { orderCode, status } = req.body;

        if (!orderCode) {
            return res.status(400).json({ error: 'Missing orderCode' });
        }

        const isSuccess = status && String(status).toUpperCase() === 'PAID';
        if (isSuccess) {
            const { rows: bookings } = await db.query(
                "SELECT id FROM bookings WHERE metadata->'payment'->>'orderCode' = $1 AND status = 'pending'",
                [String(orderCode)]
            );

            if (bookings.length > 0) {
                const bookingIds = bookings.map(b => b.id);
                 await db.query(
                    "UPDATE bookings SET status='confirmed', price_paid = total_amount WHERE id = ANY($1::bigint[])",
                    [bookingIds]
                );
                console.log(`Webhook: Updated bookings ${bookingIds.join(', ')} to confirmed`);
                return res.json({ success: true, bookings_updated: bookingIds });
            }
        }
        res.json({ success: true, message: 'Webhook processed, no action taken.' });
    } catch (err) {
        console.error('Webhook error:', err);
        res.status(500).json({ error: err.message });
    }
};

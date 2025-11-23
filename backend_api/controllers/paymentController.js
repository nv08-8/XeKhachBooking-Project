// backend_api/controllers/paymentController.js
const payos = require('../services/payos');
const db = require('../db');

// Create PayOS checkout link
exports.createCheckout = async (req, res) => {
    const orderId = req.body.orderId || req.body.order_id;
    const amount = req.body.amount;
    const booking_ids = req.body.booking_ids;

    console.log('createCheckout called with:', { orderId, amount, booking_ids });

    if (!orderId || !amount) {
        return res.status(400).json({ error: 'Missing orderId or amount' });
    }

    // Verify PayOS credentials / SDK
    if (!payos || typeof payos.createPaymentLink !== 'function') {
        console.error('PayOS SDK not initialized properly.');
        return res.status(500).json({ error: 'PayOS SDK not available. Check server logs.' });
    }

    if (!process.env.PAYOS_CLIENT_ID || !process.env.PAYOS_API_KEY || !process.env.PAYOS_CHECKSUM_KEY) {
        console.error('PayOS credentials missing!');
        return res.status(500).json({ error: 'PayOS not configured. Please contact administrator.' });
    }

    try {
        // Get buyer info from request body (optional) or use defaults
        const { buyerName, buyerEmail, buyerPhone, buyerAddress, items: clientItems } = req.body || {};

        // Calculate quantity and unit price
        const quantity = Array.isArray(booking_ids) && booking_ids.length > 0 ? booking_ids.length : 1;
        const unitPrice = Math.floor(Number(amount) / quantity) || Number(amount);

        // Ensure return/cancel URLs
        const returnUrl = process.env.PAYMENT_RETURN_URL || process.env.PAYOS_RETURN_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/return';
        const cancelUrl = process.env.PAYMENT_CANCEL_URL || process.env.PAYOS_CANCEL_URL || 'https://xekhachbooking-project.onrender.com/api/payment/payos/cancel';

        // Build items: prefer client-sent items, otherwise construct a default item
        const items = Array.isArray(clientItems) && clientItems.length > 0
            ? clientItems
            : [
                {
                    name: 'Vé xe khách',
                    quantity: quantity,
                    price: unitPrice
                }
            ];

        // Clean and validate buyer phone - must be valid format (no null/undefined)
        let cleanPhone = '0123456789'; // default
        if (buyerPhone && String(buyerPhone).trim()) {
            cleanPhone = String(buyerPhone).trim().replace(/\s+/g, '');
            // Remove country code if present
            if (cleanPhone.startsWith('+84')) {
                cleanPhone = '0' + cleanPhone.substring(3);
            } else if (cleanPhone.startsWith('84')) {
                cleanPhone = '0' + cleanPhone.substring(2);
            }
        }

        // PayOS requires ALL these fields for signature creation
        const paymentData = {
            orderCode: Number(orderId),
            amount: Number(amount),
            description: 'Thanh toan ve xe khach', // No Vietnamese chars to avoid encoding issues
            // Required buyer information (use provided or default)
            buyerName: (buyerName && String(buyerName).trim()) || 'Khach hang',
            buyerEmail: (buyerEmail && String(buyerEmail).trim()) || 'customer@xekhachbooking.com',
            buyerPhone: cleanPhone,
            buyerAddress: (buyerAddress && String(buyerAddress).trim()) || 'Viet Nam',
            // Required items array
            items: items,
            // Add expiredAt - 15 minutes from now
            expiredAt: Math.floor(Date.now() / 1000) + (15 * 60),
            returnUrl: returnUrl,
            cancelUrl: cancelUrl
        };

        // Defensive check: all required fields exist and properly formatted
        const required = ['orderCode', 'amount', 'returnUrl', 'cancelUrl', 'description', 'buyerName', 'buyerEmail', 'buyerPhone', 'buyerAddress'];
        const missing = required.filter(k => !paymentData[k] || paymentData[k] === undefined || paymentData[k] === null);
        if (missing.length) {
            console.error('Payment data missing required fields:', missing);
            return res.status(500).json({ error: 'Payment data incomplete: ' + missing.join(', ') });
        }

        // Ensure items array is valid
        if (!Array.isArray(paymentData.items) || paymentData.items.length === 0) {
            console.error('Items array is invalid');
            return res.status(500).json({ error: 'Items array must not be empty' });
        }

        // Validate each item has required fields
        for (const item of paymentData.items) {
            if (!item.name || !item.quantity || !item.price) {
                console.error('Item missing required fields:', item);
                return res.status(500).json({ error: 'Each item must have name, quantity, and price' });
            }
        }

        console.log('Creating PayOS payment link with:', {
            ...paymentData,
            buyerEmail: paymentData.buyerEmail.replace(/(.{3}).*(@.*)/, '$1***$2') // Mask email in logs
        });

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
    let transId = params.id || params.transactionId || params.transId;

    // Check if payment is successful based on redirect params
    let success = status && (String(status).toLowerCase() === 'paid' || String(status) === '00' || String(status).toLowerCase() === 'success');

    // If redirect did not provide status, try to fetch payment info from PayOS
    if (!success && orderCode && payos && typeof payos.getPaymentLinkInformation === 'function') {
        try {
            const info = await payos.getPaymentLinkInformation(orderCode);
            console.log('PayOS link info on return:', info);
            // Try to extract transaction id from SDK response
            transId = transId || info.id || info.transactionId || info.transId || (info.data && (info.data.id || info.data.transactionId));

            // Flexible checks for success fields returned by PayOS
            const s = info.status || info.resultCode || info.code || info.paymentStatus || (info.data && (info.data.status || info.data.resultCode || info.data.code || info.data.paymentStatus));
            success = s && (String(s).toLowerCase() === 'paid' || String(s) === '00' || String(s).toLowerCase() === 'success' || String(s).toLowerCase() === 'completed' || String(s).toLowerCase() === 'successfull' || String(s).toLowerCase() === 'successed');
        } catch (e) {
            console.warn('Could not fetch PayOS link info on return:', e.message || e);
        }
    }
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

    console.log('verifyPayment called with:', { orderId, transactionId });

    if (!orderId && !transactionId) {
        return res.status(400).json({ error: 'Missing orderId or transactionId' });
    }

    try {
        // Resolve booking IDs from payment_orders table
        let bookingIds = [];
        try {
            const key = orderId || transactionId;
            const { rows } = await db.query(
                'SELECT booking_ids FROM payment_orders WHERE order_code=$1 ORDER BY created_at DESC LIMIT 1',
                [key]
            );

            if (rows && rows.length && rows[0].booking_ids) {
                // booking_ids is already JSONB => array
                bookingIds = rows[0].booking_ids;

                // ensure it's always array
                if (!Array.isArray(bookingIds)) {
                    bookingIds = [bookingIds];
                }

                console.log('Found booking_ids from DB:', bookingIds);
            }
        } catch (e) {
            console.warn('Could not fetch booking_ids from DB:', e.message);
        }

        // Attempt to verify via PayOS SDK
        let payosStatus = null;
        try {
            if (orderId && typeof payos.getPaymentLinkInformation === 'function') {
                payosStatus = await payos.getPaymentLinkInformation(orderId);
                console.log('PayOS verify result:', payosStatus);
            }
        } catch (e) {
            console.warn('PayOS SDK verify failed:', e.message || e);
            payosStatus = null;
        }

        // Check if payment was successful
        const successFromPayos =
            payosStatus &&
            (
                payosStatus.status === 'PAID' ||
                String(payosStatus.status).toLowerCase() === 'paid' ||
                String(payosStatus.status).toLowerCase() === 'success' ||
                payosStatus.code === '00'
            );

        // Also accept success if deep link sent transactionId
        const considerSuccess = successFromPayos || !!transactionId;

        console.log('Payment verification:', { considerSuccess, bookingIds });

        // Update booking status if successful
        if (considerSuccess && bookingIds.length > 0) {
            for (const id of bookingIds) {
                await db.query(
                    "UPDATE bookings SET status='confirmed', payment_method='payos', payment_time=NOW() WHERE id=$1",
                    [id]
                );
                console.log(`Updated booking ${id} to confirmed`);
            }
        }

        return res.json({
            ok: true,
            booking_ids: bookingIds,
            payosStatus,
            success: considerSuccess
        });

    } catch (err) {
        console.error('verifyPayment error:', err);
        return res.status(500).json({ error: err.message || 'Verify failed' });
    }
};

// Webhook handler - PayOS will call this when payment status changes
exports.handleWebhook = async (req, res) => {
    console.log('PayOS webhook received:', JSON.stringify(req.body, null, 2));

    try {
        const data = req.body;

        // PayOS webhook structure varies, handle common fields
        const orderCode = data.orderCode || data.code || data.order_id || data.orderId;
        const status = data.status || data.paymentStatus;
        const transactionId = data.id || data.transactionId || data.transId;

        if (!orderCode) {
            console.warn('Webhook missing orderCode');
            return res.status(400).json({ error: 'Missing orderCode' });
        }

        // Check if payment is successful
        const isSuccess = status && (
            String(status).toLowerCase() === 'paid' ||
            String(status).toLowerCase() === 'success' ||
            String(status) === '00'
        );

        console.log('Webhook payment status:', { orderCode, status, isSuccess, transactionId });

        if (isSuccess) {
            // Find booking IDs from payment_orders
            const { rows } = await db.query(
                'SELECT booking_ids FROM payment_orders WHERE order_code=$1 ORDER BY created_at DESC LIMIT 1',
                [orderCode]
            );

            if (rows && rows.length && rows[0].booking_ids) {
                let bookingIds = rows[0].booking_ids;

                if (typeof bookingIds === 'string') {
                    try {
                        bookingIds = JSON.parse(bookingIds);
                    } catch (e) {
                        bookingIds = [ Number(bookingIds) ];
                    }
                } else if (typeof bookingIds === 'number') {
                    bookingIds = [ bookingIds ];
                }

                console.log('Webhook updating bookings:', bookingIds);

                // Update all bookings to confirmed
                for (const id of bookingIds) {
                    await db.query(
                        "UPDATE bookings SET status='confirmed', payment_method='payos', payment_time=NOW() WHERE id=$1",
                        [id]
                    );
                    console.log(`Webhook: Updated booking ${id} to confirmed`);
                }

                return res.json({ success: true, bookings_updated: bookingIds.length });
            } else {
                console.warn('No booking_ids found for order:', orderCode);
                return res.json({ success: false, message: 'No bookings found' });
            }
        }

        return res.json({ success: true, message: 'Payment not yet successful' });
    } catch (err) {
        console.error('Webhook error:', err);
        return res.status(500).json({ error: err.message });
    }
};


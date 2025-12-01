// backend_api/routes/paymentRoutes.js
const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');

router.post('/payos/create', paymentController.createCheckout);
router.get('/payos/return', paymentController.handleReturn);
router.get('/payos/cancel', paymentController.handleCancel);
router.post('/payos/verify', paymentController.verifyPayment);
router.post('/payos/webhook', paymentController.handleWebhook);

module.exports = router;

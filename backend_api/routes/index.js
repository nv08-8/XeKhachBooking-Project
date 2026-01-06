const express = require('express');
const router = express.Router();

// ====================================================================
// API ROUTES
// ====================================================================
const authRoutes = require('./api/authRoutes');
const bookingRoutes = require('./api/bookingRoutes');
const coinRoutes = require('./api/coinRoutes');
const driversRoutes = require('./api/driversRoutes');
const feedbackRoutes = require('./api/feedbackRoutes');
const paymentRoutes = require('./api/paymentRoutes');
const promoRoutes = require('./api/promoRoutes');
const pushRoutes = require('./api/pushRoutes');
const tripRoutes = require('./api/tripRoutes');
const seatsRoutes = require('./api/seatsRoutes');

// ====================================================================
// ADMIN ROUTES
// ====================================================================
const adminRoutes = require('./admin/adminRoutes');

// ====================================================================
// PUBLIC ROUTES
// ====================================================================
const dataRoutes = require('./public/dataRoutes');
const busImageRoutes = require('./public/busImageRoutes');
const metaRoutes = require('./public/metaRoutes');

// ====================================================================
// MOUNT ROUTES
// ====================================================================

// Authentication
router.use('/auth', authRoutes);

// Bookings & Trips
router.use('/', bookingRoutes);      // /api/bookings
router.use('/', tripRoutes);         // /api/trips
router.use('/seats', seatsRoutes);   // /api/seats

// Payment & Promotions
router.use('/payment', paymentRoutes); // /api/payment
router.use('/', promoRoutes);          // /api/promotions

// Coins & Feedback
router.use('/', coinRoutes);         // /api/coins
router.use('/', feedbackRoutes);     // /api/feedback

// Admin
router.use('/admin', adminRoutes);           // /api/admin
router.use('/admin/drivers', driversRoutes); // /api/admin/drivers

// Public
router.use('/', dataRoutes);         // /api/data
router.use('/', busImageRoutes);     // /api/bus-images
router.use('/', metaRoutes);         // /api/meta

module.exports = router;
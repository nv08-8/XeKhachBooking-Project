// backend_api/routes/mapsRoutes.js
/**
 * Google Maps API Routes
 * Provides endpoints for geocoding and reverse geocoding
 */

const express = require('express');
const router = express.Router();
const googleMapsService = require('../utils/googleMapsService');

/**
 * GET /api/maps/config
 * Get Maps API key for client-side map initialization
 */
router.get('/config', async (req, res) => {
    try {
        const config = await googleMapsService.getClientConfig();
        res.json(config);
    } catch (error) {
        console.error('Error getting Maps config:', error);
        res.status(500).json({
            error: 'Failed to get Maps configuration',
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * POST /api/maps/geocode
 * Geocode an address to coordinates
 *
 * Body:
 * {
 *     "address": "275H Phạm Ngũ Lão, Quận 1, Hồ Chí Minh"
 * }
 */
router.post('/geocode', async (req, res) => {
    try {
        const { address } = req.body;

        if (!address || typeof address !== 'string' || address.trim() === '') {
            return res.status(400).json({
                error: 'Address is required and must be a non-empty string',
                timestamp: new Date().toISOString()
            });
        }

        const result = await googleMapsService.geocodeAddress(address.trim());

        if (!result.success) {
            return res.status(400).json(result);
        }

        res.json(result);
    } catch (error) {
        console.error('Geocode error:', error);
        res.status(500).json({
            error: 'Geocoding failed',
            message: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * POST /api/maps/reverse-geocode
 * Reverse geocode coordinates to address
 *
 * Body:
 * {
 *     "latitude": 10.7769,
 *     "longitude": 106.7009
 * }
 */
router.post('/reverse-geocode', async (req, res) => {
    try {
        const { latitude, longitude } = req.body;

        // Validate coordinates
        if (
            latitude === undefined || latitude === null ||
            longitude === undefined || longitude === null ||
            typeof latitude !== 'number' || typeof longitude !== 'number'
        ) {
            return res.status(400).json({
                error: 'Latitude and longitude are required and must be numbers',
                timestamp: new Date().toISOString()
            });
        }

        if (latitude < -90 || latitude > 90) {
            return res.status(400).json({
                error: 'Latitude must be between -90 and 90',
                timestamp: new Date().toISOString()
            });
        }

        if (longitude < -180 || longitude > 180) {
            return res.status(400).json({
                error: 'Longitude must be between -180 and 180',
                timestamp: new Date().toISOString()
            });
        }

        const result = await googleMapsService.reverseGeocode(latitude, longitude);

        if (!result.success) {
            return res.status(400).json(result);
        }

        res.json(result);
    } catch (error) {
        console.error('Reverse geocode error:', error);
        res.status(500).json({
            error: 'Reverse geocoding failed',
            message: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * GET /api/maps/status
 * Get Maps API service status
 */
router.get('/status', async (req, res) => {
    try {
        const status = await googleMapsService.getStatus();
        res.json(status);
    } catch (error) {
        console.error('Error getting Maps status:', error);
        res.status(500).json({
            error: 'Failed to get Maps service status',
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * GET /api/maps/availability
 * Check if Maps API is available (not locked)
 */
router.get('/availability', async (req, res) => {
    try {
        const available = await googleMapsService.isAvailable();
        res.json({
            available,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        console.error('Error checking Maps availability:', error);
        res.status(500).json({
            available: false,
            error: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

module.exports = router;


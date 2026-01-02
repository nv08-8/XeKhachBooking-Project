// backend_api/utils/googleMapsService.js
/**
 * Google Maps Service - Simple version like sendEmail.js
 * Provides geocoding and reverse geocoding
 */

// Try to load apiKeyStatus, fallback to dummy if not available
let apiKeyStatus;
try {
    apiKeyStatus = require('./apiKeyStatus');
} catch (err) {
    apiKeyStatus = {
        acquireLock: async () => true,
        releaseLock: async () => {},
        logOperation: async () => {}
    };
}

const SERVICE_NAME = 'google_maps';

class GoogleMapsClient {
    constructor() {
        this.apiKey = process.env.GOOGLE_MAPS_API_KEY;
        this.baseUrl = 'https://maps.googleapis.com/maps/api';
    }

    async getClientConfig() {
        return {
            maps_api_key: this.apiKey || null
        };
    }

    async geocodeAddress(address) {
        try {
            // Acquire lock
            await apiKeyStatus.acquireLock(SERVICE_NAME);

            if (!this.apiKey) {
                throw new Error('GOOGLE_MAPS_API_KEY is not set');
            }

            const response = await fetch(
                `${this.baseUrl}/geocode/json?address=${encodeURIComponent(address)}&key=${this.apiKey}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            if (data.status !== 'OK') {
                console.warn(`⚠️  Geocoding failed for "${address}": ${data.status}`);
                return {
                    success: false,
                    status: data.status,
                    message: data.error_message || 'Geocoding failed'
                };
            }

            const result = data.results[0];
            const location = result.geometry.location;

            console.log(`📍 Geocoded "${address}" to (${location.lat}, ${location.lng})`);
            await apiKeyStatus.logOperation(SERVICE_NAME, `GEOCODE: ${address}`);

            return {
                success: true,
                address: result.formatted_address,
                latitude: location.lat,
                longitude: location.lng,
                placeId: result.place_id
            };

        } catch (error) {
            console.error('❌ Geocoding error:', error.message);
            await apiKeyStatus.logOperation(SERVICE_NAME, `ERROR: ${error.message}`);

            return {
                success: false,
                status: 'ERROR',
                message: error.message
            };
        } finally {
            await apiKeyStatus.releaseLock(SERVICE_NAME);
        }
    }

    async reverseGeocode(latitude, longitude) {
        try {
            // Acquire lock
            await apiKeyStatus.acquireLock(SERVICE_NAME);

            if (!this.apiKey) {
                throw new Error('GOOGLE_MAPS_API_KEY is not set');
            }

            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new Error('Invalid coordinates');
            }

            const response = await fetch(
                `${this.baseUrl}/geocode/json?latlng=${latitude},${longitude}&key=${this.apiKey}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            if (data.status !== 'OK') {
                console.warn(`⚠️  Reverse geocoding failed for (${latitude}, ${longitude}): ${data.status}`);
                return {
                    success: false,
                    status: data.status,
                    message: data.error_message || 'Reverse geocoding failed'
                };
            }

            const result = data.results[0];

            console.log(`📍 Reverse geocoded (${latitude}, ${longitude})`);
            await apiKeyStatus.logOperation(SERVICE_NAME, `REVERSE_GEOCODE: (${latitude}, ${longitude})`);

            return {
                success: true,
                address: result.formatted_address,
                placeId: result.place_id
            };

        } catch (error) {
            console.error('❌ Reverse geocoding error:', error.message);
            await apiKeyStatus.logOperation(SERVICE_NAME, `ERROR: ${error.message}`);

            return {
                success: false,
                status: 'ERROR',
                message: error.message
            };
        } finally {
            await apiKeyStatus.releaseLock(SERVICE_NAME);
        }
    }
}

const googleMaps = new GoogleMapsClient();

module.exports = googleMaps;


// backend_api/utils/googleMapsService.js
/**
 * Google Maps API Service
 * Manages Google Maps API key and provides location services
 * Includes API key status locking to prevent concurrent access issues
 */

const apiKeyStatus = require('./apiKeyStatus');

const SERVICE_NAME = 'google_maps';

class GoogleMapsService {
    constructor() {
        this.apiKey = process.env.GOOGLE_MAPS_API_KEY;
        this.baseUrl = 'https://maps.googleapis.com/maps/api';
    }

    /**
     * Get Maps API Key
     * @returns {string} API key or null if not configured
     */
    getApiKey() {
        if (!this.apiKey) {
            console.error('❌ GOOGLE_MAPS_API_KEY is not set in environment variables');
            return null;
        }
        return this.apiKey;
    }

    /**
     * Get API key status for client-side map initialization
     * @returns {Promise<Object>} API key status
     */
    async getClientConfig() {
        const SERVICE_NAME_CLIENT = 'google_maps';

        try {
            const isLocked = await apiKeyStatus.isLocked(SERVICE_NAME_CLIENT);

            return {
                maps_api_key: this.apiKey || null,
                status: isLocked ? 'LOCKED' : 'AVAILABLE',
                timestamp: new Date().toISOString()
            };
        } catch (err) {
            console.error(`Error getting client config for ${SERVICE_NAME_CLIENT}:`, err.message);
            return {
                maps_api_key: this.apiKey || null,
                status: 'UNKNOWN',
                timestamp: new Date().toISOString()
            };
        }
    }

    /**
     * Geocode address to coordinates (forward geocoding)
     * @param {string} address - Address to geocode
     * @returns {Promise<Object>} Coordinates and place data
     */
    async geocodeAddress(address) {
        const SERVICE_NAME_GEO = SERVICE_NAME;

        try {
            // Check if service is locked
            const isLocked = await apiKeyStatus.isLocked(SERVICE_NAME_GEO);
            if (isLocked) {
                console.warn(`⚠️  [${SERVICE_NAME_GEO}] Waiting - API key is locked by another operation`);
                await new Promise(resolve => setTimeout(resolve, 500));
            }

            // Acquire lock
            const lockAcquired = await apiKeyStatus.acquireLock(SERVICE_NAME_GEO);
            if (!lockAcquired) {
                console.warn(`⚠️  [${SERVICE_NAME_GEO}] Could not acquire lock - service may be overloaded`);
            }

            // Verify API key is available
            if (!this.apiKey) {
                throw new Error('GOOGLE_MAPS_API_KEY is not set');
            }

            // Use fetch to call Google Geocoding API
            const response = await fetch(
                `${this.baseUrl}/geocode/json?address=${encodeURIComponent(address)}&key=${this.apiKey}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (data.status !== 'OK') {
                console.warn(`⚠️  [${SERVICE_NAME_GEO}] Geocoding failed for "${address}": ${data.status}`);
                return {
                    success: false,
                    status: data.status,
                    message: data.error_message || 'Geocoding failed',
                    timestamp: new Date().toISOString()
                };
            }

            const result = data.results[0];
            const location = result.geometry.location;

            console.log(`📍 [${SERVICE_NAME_GEO}] Successfully geocoded "${address}" to (${location.lat}, ${location.lng})`);

            // Log successful operation
            await apiKeyStatus.logOperation(SERVICE_NAME_GEO, `GEOCODE_ADDRESS: ${address}`);

            return {
                success: true,
                address: result.formatted_address,
                latitude: location.lat,
                longitude: location.lng,
                placeId: result.place_id,
                components: result.address_components,
                timestamp: new Date().toISOString()
            };

        } catch (error) {
            console.error(`❌ [${SERVICE_NAME_GEO}] Geocoding error:`, error.message);
            await apiKeyStatus.logOperation(SERVICE_NAME_GEO, `ERROR_GEOCODING: ${error.message}`);

            return {
                success: false,
                status: 'ERROR',
                message: error.message,
                timestamp: new Date().toISOString()
            };
        } finally {
            // Release lock
            await apiKeyStatus.releaseLock(SERVICE_NAME_GEO);
        }
    }

    /**
     * Reverse geocode coordinates to address (reverse geocoding)
     * @param {number} latitude - Latitude coordinate
     * @param {number} longitude - Longitude coordinate
     * @returns {Promise<Object>} Address and place data
     */
    async reverseGeocode(latitude, longitude) {
        const SERVICE_NAME_REVERSE = SERVICE_NAME;

        try {
            // Check if service is locked
            const isLocked = await apiKeyStatus.isLocked(SERVICE_NAME_REVERSE);
            if (isLocked) {
                console.warn(`⚠️  [${SERVICE_NAME_REVERSE}] Waiting - API key is locked by another operation`);
                await new Promise(resolve => setTimeout(resolve, 500));
            }

            // Acquire lock
            const lockAcquired = await apiKeyStatus.acquireLock(SERVICE_NAME_REVERSE);
            if (!lockAcquired) {
                console.warn(`⚠️  [${SERVICE_NAME_REVERSE}] Could not acquire lock - service may be overloaded`);
            }

            // Verify API key is available
            if (!this.apiKey) {
                throw new Error('GOOGLE_MAPS_API_KEY is not set');
            }

            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new Error('Invalid coordinates');
            }

            // Use fetch to call Google Reverse Geocoding API
            const response = await fetch(
                `${this.baseUrl}/geocode/json?latlng=${latitude},${longitude}&key=${this.apiKey}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (data.status !== 'OK') {
                console.warn(`⚠️  [${SERVICE_NAME_REVERSE}] Reverse geocoding failed for (${latitude}, ${longitude}): ${data.status}`);
                return {
                    success: false,
                    status: data.status,
                    message: data.error_message || 'Reverse geocoding failed',
                    timestamp: new Date().toISOString()
                };
            }

            const result = data.results[0];

            console.log(`📍 [${SERVICE_NAME_REVERSE}] Successfully reverse geocoded (${latitude}, ${longitude})`);

            // Log successful operation
            await apiKeyStatus.logOperation(SERVICE_NAME_REVERSE, `REVERSE_GEOCODE: (${latitude}, ${longitude})`);

            return {
                success: true,
                address: result.formatted_address,
                placeId: result.place_id,
                components: result.address_components,
                timestamp: new Date().toISOString()
            };

        } catch (error) {
            console.error(`❌ [${SERVICE_NAME_REVERSE}] Reverse geocoding error:`, error.message);
            await apiKeyStatus.logOperation(SERVICE_NAME_REVERSE, `ERROR_REVERSE_GEOCODING: ${error.message}`);

            return {
                success: false,
                status: 'ERROR',
                message: error.message,
                timestamp: new Date().toISOString()
            };
        } finally {
            // Release lock
            await apiKeyStatus.releaseLock(SERVICE_NAME_REVERSE);
        }
    }

    /**
     * Check if Maps API key is configured and available
     * @returns {Promise<boolean>} True if API key is available and not locked
     */
    async isAvailable() {
        try {
            if (!this.apiKey) {
                return false;
            }

            const isLocked = await apiKeyStatus.isLocked(SERVICE_NAME);
            return !isLocked;
        } catch (err) {
            console.error('Error checking Maps API availability:', err.message);
            return false;
        }
    }

    /**
     * Get Maps API usage status
     * @returns {Promise<Object>} Usage information
     */
    async getStatus() {
        try {
            const status = await apiKeyStatus.getFullStatus();
            const mapsStatus = status.find(s => s.service === SERVICE_NAME);

            return {
                service: SERVICE_NAME,
                configured: !!this.apiKey,
                ...mapsStatus,
                timestamp: new Date().toISOString()
            };
        } catch (err) {
            console.error('Error getting Maps API status:', err.message);
            return {
                service: SERVICE_NAME,
                configured: !!this.apiKey,
                status: 'UNKNOWN',
                error: err.message,
                timestamp: new Date().toISOString()
            };
        }
    }
}

// Export singleton instance
module.exports = new GoogleMapsService();


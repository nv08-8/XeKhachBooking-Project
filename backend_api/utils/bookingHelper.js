/**
 * Utility functions for booking operations
 */

/**
 * Generate a unique booking code for ticket verification
 * Format: XK{TRIP_ID}{RANDOM_HASH} (e.g., XK99A7B2C5)
 * Can be encoded into QR code for offline verification
 */
function generateBookingCode(tripId) {
  // Generate a random hex string (6 characters)
  const randomPart = Math.random().toString(16).substring(2, 8).toUpperCase();
  // Pad trip_id with zeros to ensure it's 4 digits
  const tripPart = String(tripId).padStart(4, '0');
  return `XK${tripPart}${randomPart}`;
}

module.exports = {
  generateBookingCode
};


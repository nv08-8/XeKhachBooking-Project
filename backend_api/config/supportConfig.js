/**
 * Configuration for support and cancellation messages
 */

const SUPPORT_CONFIG = {
  // Số hotline hỗ trợ hoàn tiền khi chuyến bị hủy
  REFUND_HOTLINE: process.env.REFUND_HOTLINE || '1900-XXXX',

  // Thông báo khi chuyến bị hủy
  TRIP_CANCELLED_MESSAGE: (hotline) => `⚠️ Chuyến này đã bị hủy. Vui lòng liên hệ hotline ${hotline} để được hỗ trợ hoàn tiền.`,

  // Email config
  SUPPORT_EMAIL: process.env.SUPPORT_EMAIL || 'support@xekhachbooking.com'
};

module.exports = SUPPORT_CONFIG;


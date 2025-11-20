# PayOS Integration (backend)

This file explains how PayOS is integrated and what environment variables you must set.

## Environment variables

Set these in your `.env` or hosting dashboard:

PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...
PAYOS_CHECKSUM_KEY=...
PAYMENT_RETURN_URL=https://your-domain.com/api/payos/return
PAYMENT_CANCEL_URL=https://your-domain.com/api/payos/cancel

## Endpoints

- POST /api/payos/create
  Body: { orderId, amount, booking_ids }
  Response: { checkoutUrl }

- GET /api/payos/return
  PayOS will redirect users here after payment. The handler attempts to auto-confirm bookings and shows a small HTML page with a deep link back to the app.

- GET /api/payos/cancel
  Redirect handler when user cancels payment.

- POST /api/payos/verify
  Body: { orderId?, transactionId? }
  Verifies transaction with PayOS SDK (if available) and confirms bookings from `payment_orders` mapping.

## DB migration

Run `psql` or your migration tool with `backend_api/migrations/001_create_payment_orders.sql` to create the `payment_orders` table.

## Notes

- Do NOT store secret keys in client app. Use them only on backend.
- Consider setting up a webhook from PayOS if they support server-to-server notification for guaranteed delivery; handle signature verification.


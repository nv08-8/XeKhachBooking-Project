package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class BookingDetailActivity extends AppCompatActivity {

    private static final String TAG = "BookingDetailActivity";

    private TextView tvOperatorName, tvOrigin, tvDepartureTime,
                     tvDestination, tvPassengerName, tvPhoneNumber,
                     tvSeatNumber;
    private TextView tvPickupLocation, tvPickupAddress, tvDropoffLocation, tvDropoffAddress;
    private TextView tvCountdownTimer, tvBusType;
    private TextView tvPaymentMethodHeading;
    private View cardWaiting, cardPaymentMethod, qrCodeSection, actionButtonsContainer;
    private ImageView ivQrCode;
    private Button btnCancelTicket, btnPayTicket, btnChangePaymentMethod;
    private TextView tvQrHint;
    private MaterialToolbar toolbar;

    private ApiService apiService;
    private int bookingId;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        initializeViews();
        setupToolbar();

        apiService = ApiClient.getClient().create(ApiService.class);

        bookingId = getIntent().getIntExtra("booking_id", 0);

        if (bookingId == 0) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCancelTicket.setOnClickListener(v -> showCancelConfirmationDialog());
        btnPayTicket.setOnClickListener(v -> {
            Intent intent = new Intent(BookingDetailActivity.this, PaymentActivity.class);
            intent.putExtra("is_pending_payment", true);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(bookingId);
            intent.putIntegerArrayListExtra("booking_ids", ids);
            startActivity(intent);
        });

        btnChangePaymentMethod.setOnClickListener(v -> {
            // Re-open PaymentActivity in pending mode so user can select another method
            Intent intent = new Intent(BookingDetailActivity.this, PaymentActivity.class);
            intent.putExtra("is_pending_payment", true);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(bookingId);
            intent.putIntegerArrayListExtra("booking_ids", ids);
            startActivity(intent);
        });

        loadBookingDetails();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);

        // Waiting card
        cardWaiting = findViewById(R.id.cardWaiting);
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer);

        // Trip info
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvBusType = findViewById(R.id.tvBusType);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvDestination = findViewById(R.id.tvDestination);

        // Pickup/Dropoff
        tvPickupLocation = findViewById(R.id.tvPickupLocation);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropoffLocation = findViewById(R.id.tvDropoffLocation);
        tvDropoffAddress = findViewById(R.id.tvDropoffAddress);

        // Passenger info
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);

        // Payment method section
        tvPaymentMethodHeading = findViewById(R.id.tvPaymentMethodHeading);
        cardPaymentMethod = findViewById(R.id.cardPaymentMethod);

        // QR Code section
        qrCodeSection = findViewById(R.id.qrCodeSection);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrHint = findViewById(R.id.tvQrHint);

        // Action buttons
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
        btnCancelTicket = findViewById(R.id.btnCancelTicket);
        btnPayTicket = findViewById(R.id.btnPayTicket);
        btnChangePaymentMethod = findViewById(R.id.btnChangePaymentMethod);

        // Default: hide all optional sections
        if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
        if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
        if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);
        if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
        if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void loadBookingDetails() {
        // Show progress bar if you have one
        apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayBookingDetails(response.body());
                } else if (response.code() == 404) {
                    // Booking not found -> likely expired
                    new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                            .setTitle("Vé không còn")
                            .setMessage("Vé chờ thanh toán của bạn đã hết hạn và đã bị xóa.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(BookingDetailActivity.this, "Không thể tải thông tin vé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Load booking failed", t);
                Toast.makeText(BookingDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookingDetails(Map<String, Object> data) {
        // Operator & Bus Type
        String operator = (String) data.get("operator");
        if (tvOperatorName != null && operator != null) {
            tvOperatorName.setText(operator.toUpperCase());
        }

        String busType = (String) data.get("bus_type");
        if (tvBusType != null && busType != null) {
            tvBusType.setText(busType);
        }

        // Route and Time
        String origin = (String) data.get("origin");
        String destination = (String) data.get("destination");
        if (tvOrigin != null && origin != null) tvOrigin.setText(origin);
        if (tvDestination != null && destination != null) tvDestination.setText(destination);

        String departureTime = formatTime((String) data.get("departure_time"));
        String departureDate = formatDate((String) data.get("departure_time"));
        if (tvDepartureTime != null) {
            tvDepartureTime.setText(departureTime + " • " + departureDate);
        }

        // Pickup/Dropoff Locations
        if (tvPickupLocation != null) {
            String pickupLoc = (String) data.get("pickup_location");
            tvPickupLocation.setText(pickupLoc != null ? pickupLoc : "Điểm đón");
        }
        if (tvPickupAddress != null) {
            String pickupAddr = (String) data.get("pickup_address");
            tvPickupAddress.setText(pickupAddr != null ? pickupAddr : "");
        }
        if (tvDropoffLocation != null) {
            String dropoffLoc = (String) data.get("dropoff_location");
            tvDropoffLocation.setText(dropoffLoc != null ? dropoffLoc : "Điểm trả");
        }
        if (tvDropoffAddress != null) {
            String dropoffAddr = (String) data.get("dropoff_address");
            tvDropoffAddress.setText(dropoffAddr != null ? dropoffAddr : "");
        }

        // Passenger Info
        if (tvPassengerName != null) {
            String passengerName = (String) data.get("passenger_name");
            tvPassengerName.setText(passengerName != null ? passengerName : "N/A");
        }
        if (tvPhoneNumber != null) {
            String phone = (String) data.get("passenger_phone");
            tvPhoneNumber.setText(phone != null ? phone : "N/A");
        }

        // Seat Info
        if (tvSeatNumber != null) {
            try {
                Object seatLabelsObj = data.get("seat_labels");
                String seatText = "";
                if (seatLabelsObj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) seatLabelsObj;
                    seatText = android.text.TextUtils.join(", ", list);
                } else if (seatLabelsObj instanceof String) {
                    seatText = (String) seatLabelsObj;
                } else if (data.get("seat_label") instanceof String) {
                    seatText = (String) data.get("seat_label");
                }
                tvSeatNumber.setText(seatText);
            } catch (Exception e) {
                tvSeatNumber.setText((String) data.get("seat_label"));
            }
        }

        // Total Amount - Update button text
        Object pricePaidObj = data.get("price_paid");
        Object totalAmountObj = data.get("total_amount");
        double amount = 0.0;

        if (pricePaidObj instanceof Number) {
            amount = ((Number) pricePaidObj).doubleValue();
        } else if (pricePaidObj instanceof String) {
            try {
                amount = Double.parseDouble((String) pricePaidObj);
            } catch (NumberFormatException ignored) {}
        }

        if (amount == 0 && totalAmountObj instanceof Number) {
            amount = ((Number) totalAmountObj).doubleValue();
        } else if (amount == 0 && totalAmountObj instanceof String) {
            try {
                amount = Double.parseDouble((String) totalAmountObj);
            } catch (NumberFormatException ignored) {}
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String amountStr = formatter.format(amount);
        if (btnPayTicket != null) {
            btnPayTicket.setText("Thanh toán • " + amountStr);
        }

        // Determine payment method type
        String pm = (String) data.get("payment_method");
        String status = (String) data.get("status");
        boolean isOnlinePayment = false;

        if (pm != null) {
            String lower = pm.toLowerCase(Locale.getDefault()).trim();
            if (lower.equals("qr") || lower.equals("card") || lower.equals("payos") ||
                lower.contains("wallet") || lower.contains("e-") || lower.contains("thẻ") ||
                lower.contains("online") || lower.equals("credit") || lower.equals("debit")) {
                isOnlinePayment = true;
            }
            Log.d(TAG, "Payment method: '" + pm + "' → isOnlinePayment: " + isOnlinePayment);
        }

        // Handle Countdown if pending & online
        if ("pending".equals(status)) {
            handlePendingCountdown(data, isOnlinePayment);

            // Show payment method section
            if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.VISIBLE);
            if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.VISIBLE);

            // Show action buttons
            if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);

            // Hide QR code section
            if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);

        } else if ("confirmed".equals(status) || "completed".equals(status)) {
            // Hide waiting card and payment method
            if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
            if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
            if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);

            // Show QR code section
            if (qrCodeSection != null) qrCodeSection.setVisibility(View.VISIBLE);
            generateQRCode(String.valueOf(bookingId));

            // Show only cancel button (if booking allows cancellation)
            if (actionButtonsContainer != null) {
                actionButtonsContainer.setVisibility(View.VISIBLE);
                if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
                if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);

                // Hide cancel button for completed status (trip already happened)
                if ("completed".equals(status) && btnCancelTicket != null) {
                    btnCancelTicket.setVisibility(View.GONE);
                    actionButtonsContainer.setVisibility(View.GONE);
                }
            }

        } else {
            // cancelled or other statuses: hide everything
            if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
            if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
            if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);
            if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
            if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
        }
    }

    private void handlePendingCountdown(Map<String, Object> data, boolean isOnlinePayment) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (!isOnlinePayment) {
            // Cash/Offline payment -> hide waiting card
            if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
            return;
        }

        // Online payment (QR, Card, etc.) -> show countdown in waiting card
        String createdAtStr = (String) data.get("created_at");
        long createdMillis = -1;
        if (createdAtStr != null) {
             try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(createdAtStr);
                if (d != null) createdMillis = d.getTime();
            } catch (ParseException e) {
                // Try fallback format without millis
                try {
                     SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                     sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                     Date d2 = sdf2.parse(createdAtStr);
                     if (d2 != null) createdMillis = d2.getTime();
                } catch (ParseException ignored) {}
            }
        }

        if (createdMillis > 0) {
            long now = System.currentTimeMillis();
            long ttl = 10 * 60 * 1000L; // 10 minutes
            long expirationTime = createdMillis + ttl;
            long millisInFuture = expirationTime - now;

            if (millisInFuture > 0) {
                // Show waiting card
                if (cardWaiting != null) cardWaiting.setVisibility(View.VISIBLE);

                countDownTimer = new CountDownTimer(millisInFuture, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long seconds = millisUntilFinished / 1000;
                        long mm = seconds / 60;
                        long ss = seconds % 60;
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss);
                        if (tvCountdownTimer != null) {
                            tvCountdownTimer.setText(timeStr);
                        }
                    }

                    @Override
                    public void onFinish() {
                        // Hide waiting card, show expired message
                        if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
                        if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
                        if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.GONE);
                        if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.GONE);

                        if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);

                        Toast.makeText(BookingDetailActivity.this,
                                "Vé đã hết hạn thanh toán. Vui lòng thoát ra và kiểm tra lại.",
                                Toast.LENGTH_LONG).show();

                        // ✅ DON'T reload - it causes infinite loop!
                        // User should manually refresh or go back
                    }
                }.start();
            } else {
                 // Already expired
                 if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.GONE);
                 if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.GONE);

                 Toast.makeText(BookingDetailActivity.this,
                         "Vé đã hết hạn thanh toán",
                         Toast.LENGTH_LONG).show();
            }
        } else {
            // Could not parse created_at, hide waiting card
            if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
        }
    }

    private void generateQRCode(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Could not generate QR code", e);
        }
    }
    
    private String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    private String formatDate(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));
            return dateFormat.format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "confirmed":
                return "Đã xác nhận";
            case "cancelled":
                return "Đã hủy";
            case "pending":
                return "Chờ thanh toán";
            case "completed":
                return "Đã hoàn thành";
            case "expired":
                return "Hết hạn";
            default:
                return status;
        }
    }

    private void showCancelConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy vé")
                .setMessage("Bạn có chắc chắn muốn hủy vé này không?")
                .setPositiveButton("Hủy vé", (dialog, which) -> {
                    // Call API to cancel booking
                    cancelBooking();
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelBooking() {
        // TODO: Implement actual cancel API call
        Toast.makeText(this, "Đang hủy vé...", Toast.LENGTH_SHORT).show();

        // Example API call (uncomment when ready):
        // apiService.cancelBooking(bookingId).enqueue(new Callback<Map<String, Object>>() {
        //     @Override
        //     public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
        //         if (response.isSuccessful()) {
        //             Toast.makeText(BookingDetailActivity.this, "Đã hủy vé", Toast.LENGTH_SHORT).show();
        //             finish();
        //         } else {
        //             Toast.makeText(BookingDetailActivity.this, "Không thể hủy vé", Toast.LENGTH_SHORT).show();
        //         }
        //     }
        //
        //     @Override
        //     public void onFailure(Call<Map<String, Object>> call, Throwable t) {
        //         Toast.makeText(BookingDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
        //     }
        // });
    }
}

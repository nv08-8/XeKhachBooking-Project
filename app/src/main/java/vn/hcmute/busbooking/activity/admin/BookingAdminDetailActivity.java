package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.CurrencyUtil;
import vn.hcmute.busbooking.utils.SessionManager;

public class BookingAdminDetailActivity extends AppCompatActivity {

    private static final String TAG = "BookingAdminDetail";

    private TextView tvOperatorName, tvStatus, tvOrigin, tvDepartureTime, 
                     tvDestination, tvArrivalTime, tvDate, tvPassengerName, tvPhoneNumber,
                     tvSeatNumber, tvBookingCode, tvPaymentName, tvBasePrice, tvDiscount, tvTotalPrice,
                     tvPickupLocation, tvPickupAddress, tvDropoffLocation, tvDropoffAddress;
    private Button btnConfirmBooking, btnCancelBooking;
    private Toolbar toolbar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private int bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_admin_detail);

        initializeViews();
        setupToolbar();

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        bookingId = getIntent().getIntExtra("booking_id", 0);

        if (bookingId == 0) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnConfirmBooking.setOnClickListener(v -> handleConfirmBooking());
        btnCancelBooking.setOnClickListener(v -> handleCancelBooking());

        loadBookingDetails();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvDestination = findViewById(R.id.tvDestination);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
        tvDate = findViewById(R.id.tvDate);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);
        tvBookingCode = findViewById(R.id.tvBookingCode);
        tvPaymentName = findViewById(R.id.tvPaymentName);
        tvBasePrice = findViewById(R.id.tvBasePrice);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvPickupLocation = findViewById(R.id.tvPickupLocation);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropoffLocation = findViewById(R.id.tvDropoffLocation);
        tvDropoffAddress = findViewById(R.id.tvDropoffAddress);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBookingDetails() {
        apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayBookingDetails(response.body());
                } else {
                    Toast.makeText(BookingAdminDetailActivity.this, "Không thể tải thông tin vé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Load booking failed", t);
                Toast.makeText(BookingAdminDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookingDetails(Map<String, Object> data) {
        // Store booking data for later use

        // Operator and Bus Type
        String operator = (String) data.get("operator");
        if (operator != null) tvOperatorName.setText(operator.toUpperCase());

        // Status
        String status = (String) data.get("status");
        if (status != null) tvStatus.setText(getStatusText(status));

        // Route and Time
        String origin = (String) data.get("origin");
        String destination = (String) data.get("destination");
        if (origin != null) tvOrigin.setText(origin);
        if (destination != null) tvDestination.setText(destination);

        String departureTimeStr = (String) data.get("departure_time");
        String departureTime = formatTime(departureTimeStr);
        String departureDate = formatDate(departureTimeStr);

        Log.d(TAG, "Departure time string: " + departureTimeStr);
        Log.d(TAG, "Formatted departure time: " + departureTime);
        Log.d(TAG, "Formatted departure date: " + departureDate);

        if (tvDepartureTime != null && !departureTime.isEmpty()) {
            tvDepartureTime.setText(departureTime);
        }
        if (tvDate != null && !departureDate.isEmpty()) {
            tvDate.setText(departureDate);
        }

        String arrivalTime = formatTime((String) data.get("arrival_time"));
        if (tvArrivalTime != null && !arrivalTime.isEmpty()) {
            tvArrivalTime.setText(arrivalTime);
        }

        // Passenger Info - Prefer passenger_info object if available
        String passengerName = (String) data.get("passenger_name");
        String passengerPhone = (String) data.get("passenger_phone");

        try {
            Object pObj = data.get("passenger_info");
            if (pObj instanceof Map) {
                Map<?, ?> pm = (Map<?, ?>) pObj;
                Object n = pm.get("name");
                Object ph = pm.get("phone");
                if (n != null) passengerName = String.valueOf(n);
                if (ph != null) passengerPhone = String.valueOf(ph);
            } else if (pObj instanceof String) {
                try {
                    org.json.JSONObject jo = new org.json.JSONObject((String) pObj);
                    String n = jo.optString("name", null);
                    String ph = jo.optString("phone", null);
                    if (n != null && !n.isEmpty()) passengerName = n;
                    if (ph != null && !ph.isEmpty()) passengerPhone = ph;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        if (tvPassengerName != null) tvPassengerName.setText(passengerName != null ? passengerName : "");
        if (tvPhoneNumber != null) tvPhoneNumber.setText(passengerPhone != null ? passengerPhone : "");

        // Pickup and Dropoff Locations
        String pickupLocation = (String) data.get("pickup_location");
        if (tvPickupLocation != null) {
            tvPickupLocation.setText(pickupLocation != null && !pickupLocation.isEmpty() ? pickupLocation : "-");
        }

        String pickupAddress = (String) data.get("pickup_address");
        if (tvPickupAddress != null) {
            tvPickupAddress.setText(pickupAddress != null && !pickupAddress.isEmpty() ? pickupAddress : "");
            if (pickupAddress == null || pickupAddress.isEmpty()) {
                tvPickupAddress.setVisibility(View.GONE);
            }
        }

        String dropoffLocation = (String) data.get("dropoff_location");
        if (tvDropoffLocation != null) {
            tvDropoffLocation.setText(dropoffLocation != null && !dropoffLocation.isEmpty() ? dropoffLocation : "-");
        }

        String dropoffAddress = (String) data.get("dropoff_address");
        if (tvDropoffAddress != null) {
            tvDropoffAddress.setText(dropoffAddress != null && !dropoffAddress.isEmpty() ? dropoffAddress : "");
            if (dropoffAddress == null || dropoffAddress.isEmpty()) {
                tvDropoffAddress.setVisibility(View.GONE);
            }
        }

        // Seat Number - Process multiple formats
        if (tvSeatNumber != null) {
            try {
                Object seatLabelsObj = data.get("seat_labels");
                java.util.List<String> labels = new java.util.ArrayList<>();

                if (seatLabelsObj instanceof java.util.List) {
                    for (Object obj : (java.util.List<?>) seatLabelsObj) {
                        if (obj == null) continue;
                        if (obj instanceof String) {
                            labels.add(((String) obj).trim());
                        } else if (obj instanceof Number) {
                            labels.add(String.valueOf(obj));
                        } else if (obj instanceof Map) {
                            Map<?, ?> m = (Map<?, ?>) obj;
                            Object lbl = null;
                            if (m.containsKey("label")) lbl = m.get("label");
                            if (lbl == null && m.containsKey("seat_label")) lbl = m.get("seat_label");
                            if (lbl == null && m.containsKey("seat_code")) lbl = m.get("seat_code");
                            if (lbl == null && m.containsKey("code")) lbl = m.get("code");
                            if (lbl == null && m.containsKey("seat")) lbl = m.get("seat");
                            if (lbl != null) labels.add(String.valueOf(lbl).trim());
                            else labels.add(m.toString());
                        } else {
                            labels.add(obj.toString());
                        }
                    }
                }

                // Fallback: legacy 'seat' field
                if (labels.isEmpty()) {
                    Object seatObj = data.get("seat");
                    if (seatObj instanceof String) {
                        String s = ((String) seatObj).trim();
                        if (!s.isEmpty()) {
                            for (String p : s.split(",")) if (!p.trim().isEmpty()) labels.add(p.trim());
                        }
                    } else if (seatObj instanceof Number) {
                        labels.add(String.valueOf(seatObj));
                    }
                }

                // Remove empty & duplicates
                java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>(labels);
                String seatText = android.text.TextUtils.join(", ", unique);
                tvSeatNumber.setText(seatText.isEmpty() ? "-" : seatText);
            } catch (Exception e) {
                Log.e(TAG, "Error processing seat_labels", e);
                Object seatObj = data.get("seat_label");
                tvSeatNumber.setText(seatObj != null ? (String) seatObj : "-");
            }
        }

        // License Plate information is not displayed in admin layout

        // Booking Code
        String bookingCode = (String) data.get("booking_code");
        if (tvBookingCode != null && bookingCode != null) {
            tvBookingCode.setText(bookingCode);
        }

        // Total Amount
        Object totalAmountObj = data.get("total_amount");
        double totalAmount = 0.0;
        if (totalAmountObj instanceof String) {
            try {
                totalAmount = Double.parseDouble((String) totalAmountObj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse total_amount string: " + totalAmountObj);
            }
        } else if (totalAmountObj instanceof Number) {
            totalAmount = ((Number) totalAmountObj).doubleValue();
        }

        // Base Price
        Object basePriceObj = data.get("base_price");
        double basePrice = totalAmount;
        if (basePriceObj instanceof Number) {
            basePrice = ((Number) basePriceObj).doubleValue();
        } else if (basePriceObj instanceof String) {
            try {
                basePrice = Double.parseDouble((String) basePriceObj);
            } catch (NumberFormatException ignored) {}
        }

        // Display base price and total price
        if (tvBasePrice != null) {
            tvBasePrice.setText(CurrencyUtil.formatVND(basePrice));
        }
        if (tvTotalPrice != null) {
            tvTotalPrice.setText(CurrencyUtil.formatVND(totalAmount));
        }

        // Discount (if applicable)
        // Calculate total discount from both discount_amount (voucher) and coin_discount
        double voucherDiscount = 0.0;
        double coinDiscount = 0.0;

        Object discountObj = data.get("discount_amount");
        if (discountObj instanceof Number) {
            voucherDiscount = ((Number) discountObj).doubleValue();
        } else if (discountObj instanceof String) {
            try {
                voucherDiscount = Double.parseDouble((String) discountObj);
            } catch (NumberFormatException ignored) {}
        }

        Object coinDiscountObj = data.get("coin_discount");
        if (coinDiscountObj instanceof Number) {
            coinDiscount = ((Number) coinDiscountObj).doubleValue();
        } else if (coinDiscountObj instanceof String) {
            try {
                coinDiscount = Double.parseDouble((String) coinDiscountObj);
            } catch (NumberFormatException ignored) {}
        }

        // Total discount = voucher + coins
        double totalDiscount = voucherDiscount + coinDiscount;

        // Display discount if > 0
        if (totalDiscount > 0) {
            android.view.View discountRow = findViewById(R.id.discountRow);
            if (discountRow != null) {
                discountRow.setVisibility(View.VISIBLE);
            }
            if (tvDiscount != null) {
                tvDiscount.setText("-" + CurrencyUtil.formatVND(totalDiscount));
            }
        }

        // Payment Method
        String paymentMethod = (String) data.get("payment_method");
        if (tvPaymentName != null && paymentMethod != null) {
            String paymentDisplayName = getPaymentMethodName(paymentMethod);
            tvPaymentName.setText(paymentDisplayName);
        }

        // Determine UI by status
        if ("pending".equals(status)) {
            btnConfirmBooking.setVisibility(View.VISIBLE);
            btnCancelBooking.setVisibility(View.VISIBLE);
        } else if ("cancelled".equals(status)) {
            btnConfirmBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.GONE);
        } else {
            btnConfirmBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.VISIBLE);
        }
    }

    private String formatTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) return "";
        try {
            // Handle ISO 8601 format: both with Z and without Z
            // "2026-01-21T08:00:00.000Z" or "2026-01-21T08:00:00.000"
            String cleanDate = isoString;
            if (isoString.endsWith("Z")) {
                cleanDate = isoString.substring(0, isoString.length() - 1);
            }

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            Date date = isoFormat.parse(cleanDate);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + isoString, e);
        }
        return "";
    }

    private String formatDate(String isoString) {
        if (isoString == null || isoString.isEmpty()) return "";
        try {
            // Handle ISO 8601 format: both with Z and without Z
            // "2026-01-21T08:00:00.000Z" or "2026-01-21T08:00:00.000"
            String cleanDate = isoString;
            if (isoString.endsWith("Z")) {
                cleanDate = isoString.substring(0, isoString.length() - 1);
            }

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            Date date = isoFormat.parse(cleanDate);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + isoString, e);
        }
        return "";
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "confirmed":
                return "Đã thanh toán";
            case "cancelled":
                return "Đã hủy";
            case "pending":
                return "Chờ thanh toán";
            case "completed":
                return "Đã đi";
            case "expired":
                return "Đã hủy";
            default:
                return status;
        }
    }

    private String getPaymentMethodName(String paymentMethod) {
        if (paymentMethod == null) return "Không xác định";
        switch (paymentMethod) {
            case "card":
                return "Thẻ tín dụng";
            case "qr":
                return "QR Code (PayOS)";
            case "offline":
                return "Thanh toán tại nhà xe";
            case "momo":
                return "Momo";
            case "vnpay":
                return "VNPay";
            case "banking":
                return "Chuyển khoản ngân hàng";
            default:
                return paymentMethod;
        }
    }

    private void handleConfirmBooking() {
        if (bookingId <= 0) return;
        int adminId = sessionManager.getUserId();

        apiService.confirmAdminBooking(adminId, bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BookingAdminDetailActivity.this, "Xác nhận đặt vé thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(BookingAdminDetailActivity.this, "Lỗi khi xác nhận", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BookingAdminDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCancelBooking() {
        if (bookingId <= 0) return;
        int adminId = sessionManager.getUserId();

        apiService.cancelAdminBooking(adminId, bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BookingAdminDetailActivity.this, "Hủy đặt vé thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(BookingAdminDetailActivity.this, "Lỗi khi hủy", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BookingAdminDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

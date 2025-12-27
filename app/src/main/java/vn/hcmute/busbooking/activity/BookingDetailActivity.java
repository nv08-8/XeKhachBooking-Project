package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

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

    private TextView tvOperatorName, tvStatus, tvOrigin, tvDepartureTime, 
                     tvDestination, tvArrivalDate, tvPassengerName, tvPhoneNumber, 
                     tvSeatNumber, tvLicensePlate, tvTotalAmount;
    private ImageView ivQrCode, ivOperatorLogo;
    private Button btnCancelTicket, btnPayTicket, btnChangePaymentMethod;
    private TextView tvQrHint;
    private Toolbar toolbar;

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
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvDestination = findViewById(R.id.tvDestination);
        tvArrivalDate = findViewById(R.id.tvArrivalDate);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);
        tvLicensePlate = findViewById(R.id.tvLicensePlate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        ivQrCode = findViewById(R.id.ivQrCode);
        ivOperatorLogo = findViewById(R.id.ivOperatorLogo);
        btnCancelTicket = findViewById(R.id.btnCancelTicket);
        btnPayTicket = findViewById(R.id.btnPayTicket);
        btnChangePaymentMethod = findViewById(R.id.btnChangePaymentMethod);
        tvQrHint = findViewById(R.id.tvQrHint);

        // default hide actions/QR until loaded
        ivQrCode.setVisibility(View.GONE);
        tvQrHint.setVisibility(View.GONE);
        btnCancelTicket.setVisibility(View.GONE);
        btnPayTicket.setVisibility(View.GONE);
        btnChangePaymentMethod.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
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
        // Operator
        tvOperatorName.setText((String) data.get("operator"));
        
        // Status with color logic
        String status = (String) data.get("status");
        setStatusStyle(status);
        tvStatus.setText(getStatusText(status));

        // Route and Time
        tvOrigin.setText((String) data.get("origin"));
        tvDestination.setText((String) data.get("destination"));
        tvDepartureTime.setText(formatTime((String) data.get("departure_time")));
        tvArrivalDate.setText(formatDate((String) data.get("arrival_time")));

        // Passenger and Seat Info
        tvPassengerName.setText((String) data.get("passenger_name"));
        tvPhoneNumber.setText((String) data.get("passenger_phone"));
        // seat_labels may be an array (new schema) or legacy seat_label string
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
            tvSeatNumber.setText( (String) data.get("seat_label") );
        }
        tvLicensePlate.setText("51F-123.45"); // Placeholder, needs to come from API

        // Total Amount
        Object pricePaidObj = data.get("price_paid");
        double pricePaid = 0.0;
        if (pricePaidObj instanceof String) {
            try {
                pricePaid = Double.parseDouble((String) pricePaidObj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse price_paid string: " + pricePaidObj);
            }
        } else if (pricePaidObj instanceof Number) {
            pricePaid = ((Number) pricePaidObj).doubleValue();
        } 
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalAmount.setText(formatter.format(pricePaid));

        // Determine payment method type
        String pm = (String) data.get("payment_method");
        boolean isOnlinePayment = false;
        if (pm != null) {
            String lower = pm.toLowerCase(Locale.getDefault()).trim();
            // Check for online payment methods: qr, card, payos, wallet, etc.
            // Cash, offline, counter → isOnlinePayment = false
            if (lower.equals("qr") || lower.equals("card") || lower.equals("payos") ||
                lower.contains("wallet") || lower.contains("e-") || lower.contains("thẻ") ||
                lower.contains("online") || lower.equals("credit") || lower.equals("debit")) {
                isOnlinePayment = true;
            }
            // Explicitly set false for offline methods
            if (lower.equals("cash") || lower.equals("offline") || lower.contains("nhà xe") ||
                lower.contains("counter") || lower.contains("tại chỗ")) {
                isOnlinePayment = false;
            }
            Log.d(TAG, "Payment method: '" + pm + "' → isOnlinePayment: " + isOnlinePayment);
        } else {
            Log.w(TAG, "payment_method is null, defaulting to offline");
        }

        // Handle Countdown if pending & online
        if ("pending".equals(status)) {
            handlePendingCountdown(data, isOnlinePayment);
        }

        // Determine UI by status
        if ("pending".equals(status)) {
            // Pending
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            
            // Always show Cancel
            btnCancelTicket.setVisibility(View.VISIBLE);
            
            // Show Change Payment Method
            btnChangePaymentMethod.setVisibility(View.VISIBLE);

            if (isOnlinePayment) {
                // Online Pending: Show Pay button
                btnPayTicket.setVisibility(View.VISIBLE);
            } else {
                // Offline Pending: Hide Pay button
                btnPayTicket.setVisibility(View.GONE);
            }

        } else if ("confirmed".equals(status) || "completed".equals(status)) {
            // Confirmed/completed: show QR and Cancel (if applicable)
            ivQrCode.setVisibility(View.VISIBLE);
            tvQrHint.setVisibility(View.VISIBLE);
            generateQRCode(String.valueOf(bookingId));
            btnCancelTicket.setVisibility(View.VISIBLE);
            
            btnPayTicket.setVisibility(View.GONE);
            btnChangePaymentMethod.setVisibility(View.GONE);
        } else {
            // cancelled or other statuses: hide actions and QR
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnCancelTicket.setVisibility(View.GONE);
            btnPayTicket.setVisibility(View.GONE);
            btnChangePaymentMethod.setVisibility(View.GONE);
        }
    }

    private void handlePendingCountdown(Map<String, Object> data, boolean isOnlinePayment) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        if (!isOnlinePayment) {
            // Cash/Offline payment -> no countdown, show static text
            tvStatus.setText("Chờ thanh toán (Tại nhà xe)");
            setStatusStyle("pending");
            return;
        }

        // Online payment (QR, Card, etc.) -> show countdown
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
                countDownTimer = new CountDownTimer(millisInFuture, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long seconds = millisUntilFinished / 1000;
                        long mm = seconds / 60;
                        long ss = seconds % 60;
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss);
                        tvStatus.setText("Chờ thanh toán (" + timeStr + ")");
                    }

                    @Override
                    public void onFinish() {
                        tvStatus.setText("Hết hạn");
                        setStatusStyle("expired");
                        btnPayTicket.setVisibility(View.GONE);
                        btnCancelTicket.setVisibility(View.GONE);
                        btnChangePaymentMethod.setVisibility(View.GONE);
                        // Optionally refresh details
                        loadBookingDetails();
                    }
                }.start();
            } else {
                 tvStatus.setText("Hết hạn");
                 setStatusStyle("expired");
                 btnPayTicket.setVisibility(View.GONE);
                 btnCancelTicket.setVisibility(View.GONE);
                 btnChangePaymentMethod.setVisibility(View.GONE);
            }
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
                return "Đã thanh toán";
            case "cancelled":
                return "Đã hủy";
            case "pending":
                return "Chờ thanh toán";
            case "completed":
                return "Đã đi";
            case "expired":
                return "Hết hạn";
            default:
                return status;
        }
    }

    private void setStatusStyle(String status) {
        int backgroundColor;
        int textColor;

        if (status == null) status = "";

        switch (status) {
            case "confirmed":
                backgroundColor = ContextCompat.getColor(this, R.color.lightGreen);
                textColor = ContextCompat.getColor(this, R.color.darkGreen);
                break;
            case "completed":
                backgroundColor = ContextCompat.getColor(this, R.color.lightBlue);
                textColor = ContextCompat.getColor(this, R.color.darkBlue);
                break;
            case "pending":
                backgroundColor = ContextCompat.getColor(this, R.color.lightYellow);
                textColor = ContextCompat.getColor(this, R.color.darkYellow);
                break;
            case "cancelled":
                backgroundColor = ContextCompat.getColor(this, R.color.lightRed);
                textColor = ContextCompat.getColor(this, R.color.darkRed);
                break;
            case "expired":
                backgroundColor = ContextCompat.getColor(this, R.color.lightGray);
                textColor = ContextCompat.getColor(this, R.color.darkGray);
                break;
            default:
                backgroundColor = ContextCompat.getColor(this, R.color.lightGray);
                textColor = ContextCompat.getColor(this, R.color.darkGray);
                break;
        }

        try {
            GradientDrawable background = (GradientDrawable) tvStatus.getBackground();
            if (background != null) {
                background.setColor(backgroundColor);
            } else {
                tvStatus.setBackgroundColor(backgroundColor);
            }
        } catch (Exception e) {
             tvStatus.setBackgroundColor(backgroundColor);
        }
        tvStatus.setTextColor(textColor);
    }

    private void showCancelConfirmationDialog() {
        // Simple confirmation - in real app show AlertDialog
        Toast.makeText(this, "Hủy vé clicked", Toast.LENGTH_SHORT).show();
    }
}

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private Button btnCancelTicket, btnPayTicket;
    private TextView tvQrHint;
    private Toolbar toolbar;

    private ApiService apiService;
    private int bookingId;

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
            // also include booking-specific seat labels if present - the PaymentActivity expects seat_labels when creating payment flow
            // we can fetch seat labels from the loaded booking details via tvSeatNumber or from data map; but here we rely on PaymentActivity's pending flow which doesn't require seat_labels for pending payment
            startActivity(intent);
        });

        loadBookingDetails();
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
        tvQrHint = findViewById(R.id.tvQrHint);

        // default hide actions/QR until loaded
        ivQrCode.setVisibility(View.GONE);
        tvQrHint.setVisibility(View.GONE);
        btnCancelTicket.setVisibility(View.GONE);
        btnPayTicket.setVisibility(View.GONE);
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
        // Operator and Status
        tvOperatorName.setText((String) data.get("operator"));
        tvStatus.setText(getStatusText((String) data.get("status")));

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

        // Determine UI by status
        String status = (String) data.get("status");
        if ("pending".equals(status)) {
            // Pending: show Pay and Cancel buttons; no QR
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnPayTicket.setVisibility(View.VISIBLE);
            btnCancelTicket.setVisibility(View.VISIBLE);
        } else if ("confirmed".equals(status) || "completed".equals(status)) {
            // Confirmed/completed: show QR and Cancel (if applicable)
            ivQrCode.setVisibility(View.VISIBLE);
            tvQrHint.setVisibility(View.VISIBLE);
            generateQRCode(String.valueOf(bookingId));
            btnCancelTicket.setVisibility(View.VISIBLE);
            btnPayTicket.setVisibility(View.GONE);
        } else {
            // cancelled or other statuses: hide actions and QR
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnCancelTicket.setVisibility(View.GONE);
            btnPayTicket.setVisibility(View.GONE);
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
        // Simple confirmation - in real app show AlertDialog
        Toast.makeText(this, "Hủy vé clicked", Toast.LENGTH_SHORT).show();
    }
}

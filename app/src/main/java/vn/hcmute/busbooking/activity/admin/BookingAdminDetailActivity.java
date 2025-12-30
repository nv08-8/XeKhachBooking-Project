package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

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
import vn.hcmute.busbooking.util.CurrencyUtil;
import vn.hcmute.busbooking.utils.SessionManager;

public class BookingAdminDetailActivity extends AppCompatActivity {

    private static final String TAG = "BookingAdminDetail";

    private TextView tvOperatorName, tvStatus, tvOrigin, tvDepartureTime, 
                     tvDestination, tvArrivalTime, tvDate, tvPassengerName, tvPhoneNumber, 
                     tvSeatNumber, tvLicensePlate, tvTotalAmount;
    private ImageView ivOperatorLogo;
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
        tvLicensePlate = findViewById(R.id.tvLicensePlate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        ivOperatorLogo = findViewById(R.id.ivOperatorLogo);
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
        // Operator and Status
        tvOperatorName.setText((String) data.get("operator"));
        tvStatus.setText(getStatusText((String) data.get("status")));

        // Route and Time
        tvOrigin.setText((String) data.get("origin"));
        tvDestination.setText((String) data.get("destination"));
        tvDepartureTime.setText(formatTime((String) data.get("departure_time")));
        tvArrivalTime.setText(formatTime((String) data.get("arrival_time")));
        tvDate.setText(formatDate((String) data.get("departure_time")));

        // Passenger and Seat Info
        tvPassengerName.setText((String) data.get("passenger_name"));
        tvPhoneNumber.setText((String) data.get("passenger_phone"));
        
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
        tvTotalAmount.setText(CurrencyUtil.formatVND(totalAmount));

        // Determine UI by status
        String status = (String) data.get("status");
        if ("pending".equals(status)) {
            btnConfirmBooking.setVisibility(View.VISIBLE);
            btnCancelBooking.setVisibility(View.VISIBLE);
        } else if ("cancelled".equals(status)) {
            btnConfirmBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.GONE);
        } else {
            btnConfirmBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.VISIBLE); // Allow cancelling confirmed bookings
        }
    }

    private String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
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
            Date date = isoFormat.parse(isoString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
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

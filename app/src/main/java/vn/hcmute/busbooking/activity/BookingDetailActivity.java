package vn.hcmute.busbooking.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.NumberFormat;
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

public class BookingDetailActivity extends AppCompatActivity {

    private TextView tvBookingId, tvStatus, tvRoute, tvDepartureTime, tvArrivalTime;
    private TextView tvOperator, tvSeat, tvAmount, tvPaymentMethod, tvCreatedAt;
    private ImageView ivQrCode;
    private ProgressBar progressBar;
    private View layoutQrCode;
    private Button btnBack;

    private ApiService apiService;
    private int bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Initialize views
        tvBookingId = findViewById(R.id.tvBookingId);
        tvStatus = findViewById(R.id.tvStatus);
        tvRoute = findViewById(R.id.tvRoute);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
        tvOperator = findViewById(R.id.tvOperator);
        tvSeat = findViewById(R.id.tvSeat);
        tvAmount = findViewById(R.id.tvAmount);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        ivQrCode = findViewById(R.id.ivQrCode);
        progressBar = findViewById(R.id.progressBar);
        layoutQrCode = findViewById(R.id.layoutQrCode);
        btnBack = findViewById(R.id.btnBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        bookingId = getIntent().getIntExtra("booking_id", 0);

        if (bookingId == 0) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());

        loadBookingDetails();
    }

    private void loadBookingDetails() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    displayBookingDetails(response.body());
                } else {
                    Toast.makeText(BookingDetailActivity.this, "Không thể tải thông tin vé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookingDetails(Map<String, Object> booking) {
        tvBookingId.setText("Mã vé: #" + booking.get("id"));

        String status = (String) booking.get("status");
        tvStatus.setText(getStatusText(status));
        tvStatus.setTextColor(getStatusColor(status));

        tvRoute.setText(booking.get("origin") + " → " + booking.get("destination"));
        tvDepartureTime.setText("Khởi hành: " + formatDateTime((String) booking.get("departure_time")));
        tvArrivalTime.setText("Đến nơi: " + formatDateTime((String) booking.get("arrival_time")));
        tvOperator.setText("Nhà xe: " + booking.get("operator"));
        tvSeat.setText("Ghế: " + booking.get("seat_label"));

        Object pricePaidObj = booking.get("price_paid");
        int pricePaid = 0;
        if (pricePaidObj instanceof Double) {
            pricePaid = ((Double) pricePaidObj).intValue();
        } else if (pricePaidObj instanceof Integer) {
            pricePaid = (Integer) pricePaidObj;
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvAmount.setText("Giá vé: " + formatter.format(pricePaid));

        String paymentMethod = (String) booking.get("payment_method");
        if (paymentMethod != null) {
            tvPaymentMethod.setText("Thanh toán: " + (paymentMethod.equals("momo") ? "MoMo" : "Ngân hàng"));
            tvPaymentMethod.setVisibility(View.VISIBLE);
        } else {
            tvPaymentMethod.setVisibility(View.GONE);
        }

        tvCreatedAt.setText("Đặt lúc: " + formatDateTime((String) booking.get("created_at")));

        // Show QR code only if payment is confirmed
        String qrCode = (String) booking.get("qr_code");
        if ("confirmed".equals(status) && qrCode != null && !qrCode.isEmpty()) {
            layoutQrCode.setVisibility(View.VISIBLE);
            generateQRCode(qrCode);
        } else {
            layoutQrCode.setVisibility(View.GONE);
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "pending":
                return "Chờ thanh toán";
            case "confirmed":
                return "Đã xác nhận";
            case "cancelled":
                return "Đã hủy";
            default:
                return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "pending":
                return getResources().getColor(android.R.color.holo_orange_dark);
            case "confirmed":
                return getResources().getColor(android.R.color.holo_green_dark);
            case "cancelled":
                return getResources().getColor(android.R.color.holo_red_dark);
            default:
                return getResources().getColor(android.R.color.black);
        }
    }

    private String formatDateTime(String dateTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private void generateQRCode(String qrData) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 400, 400);

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }
}


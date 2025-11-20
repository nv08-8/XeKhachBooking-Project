package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final String TAG = "BookingDetailActivity";

    private TextView tvBookingId, tvStatus, tvRoute, tvDepartureTime, tvArrivalTime;
    private TextView tvOperator, tvSeat, tvAmount, tvPaymentMethod, tvCreatedAt;
    private TextView tvStatusBadge, tvBusType, tvNextStepTitle, tvNextStepContent;
    private ImageView ivQrCode, ivShareHint;
    private ProgressBar progressBar;
    private View layoutQrCode;
    private Button btnBack, btnDownloadTicket;
    private View cardNextSteps;
    private ImageButton btnShareTicket;

    private ApiService apiService;
    private int bookingId;

    private String currentStatus;
    private String qrCodeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Initialize views
        tvBookingId = findViewById(R.id.tvBookingId);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvRoute = findViewById(R.id.tvRoute);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
        tvOperator = findViewById(R.id.tvOperator);
        tvSeat = findViewById(R.id.tvSeat);
        tvAmount = findViewById(R.id.tvAmount);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvBusType = findViewById(R.id.tvBusType);
        tvNextStepTitle = findViewById(R.id.tvNextStepTitle);
        tvNextStepContent = findViewById(R.id.tvNextStepContent);
        ivQrCode = findViewById(R.id.ivQrCode);
        progressBar = findViewById(R.id.progressBar);
        layoutQrCode = findViewById(R.id.layoutQrCode);
        btnBack = findViewById(R.id.btnBack);
        btnDownloadTicket = findViewById(R.id.btnDownloadTicket);
        cardNextSteps = findViewById(R.id.cardNextSteps);
        btnShareTicket = findViewById(R.id.btnShareTicket);

        apiService = ApiClient.getClient().create(ApiService.class);

        bookingId = getIntent().getIntExtra("booking_id", 0);

        if (bookingId == 0) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnShareTicket.setOnClickListener(v -> shareTicket());
        btnDownloadTicket.setOnClickListener(v -> saveTicketImage());

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

        currentStatus = (String) booking.get("status");
        tvStatus.setText(getStatusText(currentStatus));
        tvStatus.setTextColor(getStatusColor(currentStatus));
        updateStatusBadge();

        tvRoute.setText(booking.get("origin") + " → " + booking.get("destination"));
        tvDepartureTime.setText("Khởi hành: " + formatDateTime((String) booking.get("departure_time")));
        tvArrivalTime.setText("Đến nơi: " + formatDateTime((String) booking.get("arrival_time")));
        tvOperator.setText("Nhà xe: " + booking.get("operator"));
        tvSeat.setText("Ghế: " + booking.get("seat_label"));

        Object pricePaidObj = booking.get("price_paid");
        double pricePaid = 0.0;
        if (pricePaidObj instanceof Number) {
            pricePaid = ((Number) pricePaidObj).doubleValue();
        } else if (pricePaidObj instanceof String) {
            try {
                pricePaid = Double.parseDouble((String) pricePaidObj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse price_paid string: " + pricePaidObj);
            }
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
        configureNextSteps();

        qrCodeData = (String) booking.get("qr_code");
        if ("confirmed".equals(currentStatus) && qrCodeData != null && !qrCodeData.isEmpty()) {
            layoutQrCode.setVisibility(View.VISIBLE);
            btnShareTicket.setVisibility(View.VISIBLE);
            btnDownloadTicket.setVisibility(View.VISIBLE);
            generateQRCode(qrCodeData);
        } else {
            layoutQrCode.setVisibility(View.GONE);
            btnShareTicket.setVisibility(View.GONE);
            btnDownloadTicket.setVisibility(View.GONE);
        }

        String busType = (String) booking.get("bus_type");
        if (busType != null && !busType.isEmpty()) {
            tvBusType.setText("Loại xe: " + busType);
            tvBusType.setVisibility(View.VISIBLE);
        } else {
            tvBusType.setVisibility(View.GONE);
        }
    }

    private void updateStatusBadge() {
        if (tvStatusBadge == null) return;
        tvStatusBadge.setText(getStatusText(currentStatus));
        int colorRes;
        switch (currentStatus) {
            case "pending":
                colorRes = R.color.colorAccent;
                break;
            case "confirmed":
                colorRes = R.color.colorPrimary;
                break;
            case "cancelled":
            default:
                colorRes = R.color.colorError;
                break;
        }
        tvStatusBadge.getBackground().setTint(ContextCompat.getColor(this, colorRes));
    }

    private void configureNextSteps() {
        if (cardNextSteps == null) return;
        if ("pending".equals(currentStatus)) {
            cardNextSteps.setVisibility(View.VISIBLE);
            tvNextStepTitle.setText("Hoàn tất thanh toán");
            tvNextStepContent.setText("Vui lòng thanh toán trước giờ khởi hành để giữ chỗ.");
        } else if ("confirmed".equals(currentStatus)) {
            cardNextSteps.setVisibility(View.VISIBLE);
            tvNextStepTitle.setText("Chuẩn bị lên xe");
            tvNextStepContent.setText("Có mặt trước giờ khởi hành 15 phút và mang giấy tờ tùy thân.");
        } else {
            cardNextSteps.setVisibility(View.GONE);
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

    private void shareTicket() {
        if (qrCodeData == null) return;
        File imageFile = createTicketImage();
        if (imageFile == null) {
            Snackbar.make(btnShareTicket, "Không thể chia sẻ vé", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Chia sẻ vé"));
    }

    private void saveTicketImage() {
        File imageFile = createTicketImage();
        if (imageFile == null) {
            Snackbar.make(btnDownloadTicket, "Lưu vé thất bại", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Snackbar.make(btnDownloadTicket, "Đã lưu vé tại " + imageFile.getAbsolutePath(), Snackbar.LENGTH_LONG).show();
    }

    private File createTicketImage() {
        layoutQrCode.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(layoutQrCode.getWidth(), layoutQrCode.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layoutQrCode.draw(canvas);
        layoutQrCode.setDrawingCacheEnabled(false);

        try {
            File outputDir = new File(getExternalFilesDir(null), "tickets");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File ticketFile = new File(outputDir, "ticket_" + bookingId + ".png");
            FileOutputStream fos = new FileOutputStream(ticketFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();
            return ticketFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save ticket image", e);
            return null;
        }
    }
}

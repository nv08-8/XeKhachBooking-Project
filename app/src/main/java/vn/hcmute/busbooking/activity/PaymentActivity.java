package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PaymentRequest;
import vn.hcmute.busbooking.model.PaymentResponse;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvBookingId, tvRoute, tvSeat, tvAmount;
    private RadioGroup rgPaymentMethod;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;

    private final List<Integer> bookingIds = new ArrayList<>();
    private ArrayList<String> seatLabels;
    private String origin;
    private String destination;
    private String operator;
    private int amount;
    private int primaryBookingId;
    private ApiService apiService;
    private vn.hcmute.busbooking.utils.SessionManager sessionManager;

    private boolean isReturn;
    private String returnOrigin, returnDestination, returnDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("PaymentActivity", "onCreate started");
        setContentView(R.layout.activity_payment);

        // Initialize views
        tvBookingId = findViewById(R.id.tvBookingId);
        tvRoute = findViewById(R.id.tvRoute);
        tvSeat = findViewById(R.id.tvSeat);
        tvAmount = findViewById(R.id.tvAmount);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        progressBar = findViewById(R.id.progressBar);

        android.util.Log.d("PaymentActivity", "Views initialized");

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new vn.hcmute.busbooking.utils.SessionManager(this);

        collectIntentData();
        android.util.Log.d("PaymentActivity", "Intent data collected. BookingIds: " + bookingIds.size());

        if (!validateBookingData()) {
            android.util.Log.e("PaymentActivity", "Booking data validation failed!");
            finish();
            return;
        }

        populateBookingSummary();
        android.util.Log.d("PaymentActivity", "Summary populated");

        btnConfirmPayment.setOnClickListener(v -> processPayment());

        android.util.Log.d("PaymentActivity", "onCreate completed successfully");
    }

    private void collectIntentData() {
        ArrayList<Integer> incomingIds = getIntent().getIntegerArrayListExtra("booking_ids");
        if (incomingIds != null) {
            bookingIds.addAll(incomingIds);
        }

        int singleId = getIntent().getIntExtra("booking_id", 0);
        if (bookingIds.isEmpty() && singleId > 0) {
            bookingIds.add(singleId);
        }

        primaryBookingId = bookingIds.isEmpty() ? 0 : bookingIds.get(0);
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");
        origin = getIntent().getStringExtra("origin");
        destination = getIntent().getStringExtra("destination");
        operator = getIntent().getStringExtra("operator");

        // Get return trip information
        isReturn = getIntent().getBooleanExtra("isReturn", false);
        returnOrigin = getIntent().getStringExtra("returnOrigin");
        returnDestination = getIntent().getStringExtra("returnDestination");
        returnDate = getIntent().getStringExtra("returnDate");
        amount = getIntent().getIntExtra("amount", 0);
    }

    private boolean validateBookingData() {
        if (bookingIds.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đặt vé", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void populateBookingSummary() {
        if (bookingIds.size() == 1) {
            tvBookingId.setText("Mã đặt vé: #" + bookingIds.get(0));
        } else {
            tvBookingId.setText("Mã " + bookingIds.size() + " vé: #" + bookingIds.get(0) + " +" + (bookingIds.size() - 1));
        }

        StringBuilder routeBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(origin) && !TextUtils.isEmpty(destination)) {
            routeBuilder.append(origin).append(" → ").append(destination);
        }
        if (!TextUtils.isEmpty(operator)) {
            if (routeBuilder.length() > 0) {
                routeBuilder.append("\n");
            }
            routeBuilder.append("Nhà xe: ").append(operator);
        }
        tvRoute.setText(routeBuilder.length() == 0 ? "Tuyến đường" : routeBuilder.toString());

        String seatText;
        if (seatLabels != null && !seatLabels.isEmpty()) {
            seatText = TextUtils.join(", ", seatLabels);
        } else {
            seatText = "Đang cập nhật";
        }
        tvSeat.setText("Ghế: " + seatText);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvAmount.setText("Số tiền: " + formatter.format(amount));
    }

    private void processPayment() {
        int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
        android.util.Log.d("PaymentActivity", "processPayment: selectedId = " + selectedId);

        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmPayment.setEnabled(false);

        if (selectedId == R.id.rbPayos) {
            android.util.Log.d("PaymentActivity", "Processing PayOS payment...");
            // Thanh toán online qua PayOS (Ngân hàng, MOMO, ZaloPay...)
            processPayosPayment();
        } else {
            android.util.Log.d("PaymentActivity", "Processing Cash payment...");
            // Thanh toán tiền mặt khi lên xe
            processCashPayment();
        }
    }

    private void processPayosPayment() {
        // Tạo orderId unique
        String orderId = String.valueOf(System.currentTimeMillis());

        // Lấy thông tin user từ SessionManager
        String userName = sessionManager.getUserName();
        String userEmail = sessionManager.getUserEmail();
        String userPhone = sessionManager.getUserPhone();

        android.util.Log.d("PaymentActivity", "User info - Name: " + userName + ", Email: " + userEmail + ", Phone: " + userPhone);

        // Tạo request với thông tin user (nếu có)
        PaymentRequest request;
        if (userName != null && userEmail != null) {
            request = new PaymentRequest(orderId, amount, bookingIds, userName, userEmail, userPhone);
            android.util.Log.d("PaymentActivity", "Using user profile info for PayOS");
        } else {
            request = new PaymentRequest(orderId, amount, bookingIds);
            android.util.Log.d("PaymentActivity", "Using default buyer info (user not logged in)");
        }

        apiService.createPayosPayment(request).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnConfirmPayment.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String checkoutUrl = response.body().getCheckoutUrl();
                    if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                        // ⭐ LƯU THÔNG TIN KHỨ HỒI VÀO SHAREDPREFERENCES
                        if (isReturn && returnOrigin != null && returnDestination != null) {
                            android.content.SharedPreferences prefs = getSharedPreferences("return_trip", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("isReturn", true)
                                .putString("returnOrigin", returnOrigin)
                                .putString("returnDestination", returnDestination)
                                .putString("returnDate", returnDate)
                                .apply();
                        }

                        // Mở link thanh toán bằng trình duyệt
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                        startActivity(browserIntent);

                        Toast.makeText(PaymentActivity.this,
                            "Vui lòng hoàn tất thanh toán trên trang web và quay lại ứng dụng khi xong",
                            Toast.LENGTH_LONG).show();

                        // DON'T navigate back to MyBookings here. Leave the app and wait for the payment provider
                        // to redirect back to our app via deep link (handled in PaymentReturnActivity).
                        // Previously the code navigated back immediately which marked bookings as completed prematurely.

                        // Close PaymentActivity to let user continue in browser and return via deep link
                        finish();
                    } else {
                        Toast.makeText(PaymentActivity.this, "Không nhận được link thanh toán", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String error = "Không thể tạo link thanh toán";
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirmPayment.setEnabled(true);
                Toast.makeText(PaymentActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processCashPayment() {
        // Thanh toán tiền mặt - confirm từng booking
        confirmNextPayment(0, "cash");
    }

    private void confirmNextPayment(int index, String paymentMethod) {
        if (index >= bookingIds.size()) {
            onAllPaymentsSuccess();
            return;
        }

        int currentBookingId = bookingIds.get(index);
        Map<String, String> body = new HashMap<>();
        body.put("payment_method", paymentMethod);

        apiService.confirmPayment(currentBookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    confirmNextPayment(index + 1, paymentMethod);
                } else {
                    handlePaymentError();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handlePaymentError();
            }
        });
    }

    private void handlePaymentError() {
        progressBar.setVisibility(View.GONE);
        btnConfirmPayment.setEnabled(true);
        Toast.makeText(PaymentActivity.this, "Thanh toán thất bại", Toast.LENGTH_SHORT).show();
    }

    private void onAllPaymentsSuccess() {
        progressBar.setVisibility(View.GONE);
        btnConfirmPayment.setEnabled(true);
        Toast.makeText(PaymentActivity.this, "Đặt vé thành công! Thanh toán khi lên xe", Toast.LENGTH_LONG).show();

        // ⭐ KIỂM TRA XEM CÓ PHẢI CHUYẾN KHỨ HỒI KHÔNG
        if (isReturn && returnOrigin != null && returnDestination != null) {
            Toast.makeText(PaymentActivity.this, "Tiếp tục chọn chuyến về", Toast.LENGTH_LONG).show();

            // Mở màn hình tìm chuyến về
            Intent intent = new Intent(PaymentActivity.this, TripListActivity.class);
            intent.putExtra("origin", returnOrigin);
            intent.putExtra("destination", returnDestination);
            intent.putExtra("date", returnDate);
            intent.putExtra("isReturn", false); // Chuyến về không còn là round trip nữa
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            // Chuyến một chiều hoặc đã hoàn thành cả 2 chiều
            Intent intent = new Intent(PaymentActivity.this, BookingDetailActivity.class);
            intent.putExtra("booking_id", primaryBookingId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
}

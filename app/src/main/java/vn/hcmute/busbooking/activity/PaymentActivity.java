package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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

public class PaymentActivity extends AppCompatActivity {

    private TextView tvBookingId, tvRoute, tvSeat, tvAmount;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbMomo, rbBank;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize views
        tvBookingId = findViewById(R.id.tvBookingId);
        tvRoute = findViewById(R.id.tvRoute);
        tvSeat = findViewById(R.id.tvSeat);
        tvAmount = findViewById(R.id.tvAmount);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbMomo = findViewById(R.id.rbMomo);
        rbBank = findViewById(R.id.rbBank);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        progressBar = findViewById(R.id.progressBar);

        apiService = ApiClient.getClient().create(ApiService.class);

        collectIntentData();
        if (!validateBookingData()) {
            finish();
            return;
        }
        populateBookingSummary();

        btnConfirmPayment.setOnClickListener(v -> processPayment());
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
        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = selectedId == R.id.rbMomo ? "momo" : "bank";

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmPayment.setEnabled(false);

        confirmNextPayment(0, paymentMethod);
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
        Toast.makeText(PaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(PaymentActivity.this, BookingDetailActivity.class);
        intent.putExtra("booking_id", primaryBookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}

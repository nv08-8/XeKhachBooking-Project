package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.HashMap;
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

    private int bookingId;
    private int amount;
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

        // Get data from intent
        bookingId = getIntent().getIntExtra("booking_id", 0);
        String origin = getIntent().getStringExtra("origin");
        String destination = getIntent().getStringExtra("destination");
        String seatLabel = getIntent().getStringExtra("seat_label");
        amount = getIntent().getIntExtra("amount", 0);

        // Display booking info
        tvBookingId.setText("Mã đặt vé: #" + bookingId);
        tvRoute.setText(origin + " → " + destination);
        tvSeat.setText("Ghế: " + seatLabel);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvAmount.setText("Số tiền: " + formatter.format(amount));

        btnConfirmPayment.setOnClickListener(v -> processPayment());
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

        Map<String, String> body = new HashMap<>();
        body.put("payment_method", paymentMethod);

        apiService.confirmPayment(bookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnConfirmPayment.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    String qrCode = (String) result.get("qr_code");

                    Toast.makeText(PaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

                    // Navigate to booking detail with QR code
                    Intent intent = new Intent(PaymentActivity.this, BookingDetailActivity.class);
                    intent.putExtra("booking_id", bookingId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(PaymentActivity.this, "Thanh toán thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirmPayment.setEnabled(true);
                Toast.makeText(PaymentActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


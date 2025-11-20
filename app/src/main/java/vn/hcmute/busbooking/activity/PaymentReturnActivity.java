package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class PaymentReturnActivity extends AppCompatActivity {

    private static final String TAG = "PaymentReturnActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data == null) {
            finish();
            return;
        }

        // Example: xekhachbooking://payment?order=ORD123&transactionId=TXN456&status=success
        String order = data.getQueryParameter("order");
        String transactionId = data.getQueryParameter("transactionId");
        String status = data.getQueryParameter("status");

        if (order == null && transactionId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Call backend verify endpoint
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        if (order != null) body.put("orderId", order);
        if (transactionId != null) body.put("transactionId", transactionId);

        api.verifyPayos(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Navigate to booking detail of first booking if available
                    Object bookingIdsObj = response.body().get("booking_ids");
                    if (bookingIdsObj instanceof java.util.List && !((java.util.List) bookingIdsObj).isEmpty()) {
                        Double idDouble = (Double) ((java.util.List) bookingIdsObj).get(0);
                        int bookingId = idDouble.intValue();
                        Intent intent = new Intent(PaymentReturnActivity.this, BookingDetailActivity.class);
                        intent.putExtra("booking_id", bookingId);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
                // fallback: open MyBookings
                Intent intent = new Intent(PaymentReturnActivity.this, MyBookingsActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Verify failed", t);
                Intent intent = new Intent(PaymentReturnActivity.this, MyBookingsActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}


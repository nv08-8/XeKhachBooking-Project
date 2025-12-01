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

        String order = data.getQueryParameter("order");
        String transactionId = data.getQueryParameter("transactionId");

        if (order == null) {
            Toast.makeText(this, "Không tìm thấy thông tin giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Call backend verify endpoint
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("orderId", order);
        if (transactionId != null) {
            body.put("transactionId", transactionId);
        }

        api.verifyPayos(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Always clear return trip info, as payment is now complete
                android.content.SharedPreferences prefs = getSharedPreferences("return_trip", MODE_PRIVATE);
                prefs.edit().clear().apply();

                if (response.isSuccessful()) {
                    Toast.makeText(PaymentReturnActivity.this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(PaymentReturnActivity.this, "Xác thực thanh toán thất bại", Toast.LENGTH_SHORT).show();
                }

                // Redirect to MyBookings to show updated status
                Intent intent = new Intent(PaymentReturnActivity.this, MyBookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Verify failed", t);
                
                // Also clear return trip info on failure to prevent stale data
                android.content.SharedPreferences prefs = getSharedPreferences("return_trip", MODE_PRIVATE);
                prefs.edit().clear().apply();
                
                Toast.makeText(PaymentReturnActivity.this, "Lỗi xác thực thanh toán: " + t.getMessage(), Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(PaymentReturnActivity.this, MyBookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}

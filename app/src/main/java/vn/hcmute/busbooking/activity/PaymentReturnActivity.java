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
                    Log.d(TAG, "Verify response body: " + response.body());

                    if (bookingIdsObj instanceof java.util.List && !((java.util.List) bookingIdsObj).isEmpty()) {
                        Object first = ((java.util.List) bookingIdsObj).get(0);
                        int bookingId = -1;
                        try {
                            if (first instanceof Number) {
                                bookingId = ((Number) first).intValue();
                            } else if (first instanceof String) {
                                bookingId = Integer.parseInt((String) first);
                            } else {
                                Log.w(TAG, "Unknown booking id type: " + (first != null ? first.getClass() : "null"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse booking id", e);
                        }

                        if (bookingId > 0) {
                            // ⭐ KIỂM TRA XEM CÓ PHẢI CHUYẾN KHỨ HỒI KHÔNG
                            android.content.SharedPreferences prefs = getSharedPreferences("return_trip", MODE_PRIVATE);
                            boolean isReturn = prefs.getBoolean("isReturn", false);
                            String returnOrigin = prefs.getString("returnOrigin", null);
                            String returnDestination = prefs.getString("returnDestination", null);
                            String returnDate = prefs.getString("returnDate", null);

                            if (isReturn && returnOrigin != null && returnDestination != null) {
                                // Xóa thông tin khứ hồi
                                prefs.edit().clear().apply();

                                Toast.makeText(PaymentReturnActivity.this, "Thanh toán thành công! Tiếp tục chọn chuyến về", Toast.LENGTH_LONG).show();

                                // Mở màn hình tìm chuyến về
                                Intent intent = new Intent(PaymentReturnActivity.this, TripListActivity.class);
                                intent.putExtra("origin", returnOrigin);
                                intent.putExtra("destination", returnDestination);
                                intent.putExtra("date", returnDate);
                                intent.putExtra("isReturn", false);
                                startActivity(intent);
                                finish();
                                return;
                            }

                            // Không phải chuyến khứ hồi - mở chi tiết booking
                            Intent intent = new Intent(PaymentReturnActivity.this, BookingDetailActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            startActivity(intent);
                            finish();
                            return;
                        }
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

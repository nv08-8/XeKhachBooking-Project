package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class BookingAdminDetailActivity extends AppCompatActivity {

    private TextView tvBookingInfo;
    private Button btnConfirm, btnCancel;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int bookingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_admin_detail);

        tvBookingInfo = findViewById(R.id.tvBookingInfo);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        btnCancel = findViewById(R.id.btnCancelBooking);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        if (getIntent() != null) {
            bookingId = getIntent().getIntExtra("booking_id", -1);
            String bookingJson = getIntent().getStringExtra("booking_json");
            if (bookingJson != null) {
                tvBookingInfo.setText(prettyBookingInfo(bookingJson));
            } else {
                tvBookingInfo.setText("Booking ID: " + bookingId);
            }
        }

        btnConfirm.setOnClickListener(v -> {
            if (bookingId <= 0) return;
            int adminId = sessionManager.getUserId();
            // Use admin confirm endpoint
            apiService.confirmAdminBooking(adminId, bookingId).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(BookingAdminDetailActivity.this, "Xác nhận thanh toán thành công", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(BookingAdminDetailActivity.this, "Xác nhận thất bại (code=" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                    Toast.makeText(BookingAdminDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> {
            if (bookingId <= 0) return;
            int adminId = sessionManager.getUserId();
            apiService.cancelAdminBooking(adminId, bookingId).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(BookingAdminDetailActivity.this, "Hủy đặt vé thành công", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(BookingAdminDetailActivity.this, "Hủy thất bại (code=" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                    Toast.makeText(BookingAdminDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String prettyBookingInfo(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            StringBuilder sb = new StringBuilder();
            sb.append("Booking #").append(obj.optString("id", "?")).append("\n");
            sb.append("User: ").append(obj.optString("name", "?")).append(" (id:").append(obj.optString("user_id", "?")).append(")\n");
            sb.append("Status: ").append(obj.optString("status", "?")).append("\n");
            sb.append("Payment: ").append(obj.optString("payment_method", "?")).append(" - ").append(obj.optString("total_amount", "0")).append("\n");
            sb.append("Route: ").append(obj.optString("origin", "?")).append(" → ").append(obj.optString("destination", "?")).append("\n");
            sb.append("Pickup: ").append(obj.optString("pickup_location", "?")).append("\n");
            sb.append("Dropoff: ").append(obj.optString("dropoff_location", "?")).append("\n");
            // seat_labels may be array
            JSONArray arr = obj.optJSONArray("seat_labels");
            if (arr != null) {
                List<String> s = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) s.add(arr.optString(i));
                sb.append("Seats: ").append(String.join(", ", s)).append("\n");
            } else {
                Object seats = obj.opt("seat_labels");
                sb.append("Seats: ").append(String.valueOf(seats)).append("\n");
            }
            sb.append("Created: ").append(obj.optString("created_at", "?")).append("\n");
            return sb.toString();
        } catch (Exception e) {
            return json;
        }
    }
}

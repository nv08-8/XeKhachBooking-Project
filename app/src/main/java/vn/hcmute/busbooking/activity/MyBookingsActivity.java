package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Added this import

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.BookingAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.utils.SessionManager;

public class MyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "MyBookingsActivity";
    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private BookingAdapter bookingAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(new ArrayList<>(), this::onCancelBooking);
        bookingAdapter.setOnItemClickListener(this::onBookingClick);
        rvBookings.setAdapter(bookingAdapter);

        loadMyBookings();
    }

    private void onBookingClick(Map<String, Object> booking) {
        Object idObj = booking.get("id");
        if (idObj == null) return;

        int bookingId = 0;
        if (idObj instanceof Double) {
            bookingId = ((Double) idObj).intValue();
        } else if (idObj instanceof Integer) {
            bookingId = (Integer) idObj;
        }

        Intent intent = new Intent(this, BookingDetailActivity.class);
        intent.putExtra("booking_id", bookingId);
        startActivity(intent);
    }

    private void loadMyBookings() {
        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        apiService.getMyBookings(userId).enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Booking> bookings = response.body();
                    if (bookings.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Bạn chưa có vé nào");
                    } else {
                        bookingAdapter.updateBookings(bookings);
                    }
                    Log.d(TAG, "Loaded " + bookings.size() + " bookings");
                } else {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Không thể tải danh sách vé");
                    Log.e(TAG, "Failed to load bookings: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Lỗi kết nối: " + t.getMessage());
                Log.e(TAG, "Error loading bookings", t);
            }
        });
    }

    private void onCancelBooking(Booking booking) {
        int bookingId = booking.getId();
        String status = booking.getStatus();

        if (!"confirmed".equals(status) && !"pending".equals(status)) {
            Toast.makeText(this, "Không thể hủy vé này", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy vé")
                .setMessage("Bạn có chắc muốn hủy vé này?")
                .setPositiveButton("Hủy vé", (dialog, which) -> cancelBooking(bookingId))
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelBooking(int bookingId) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.cancelBooking(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(MyBookingsActivity.this, "Đã hủy vé thành công", Toast.LENGTH_SHORT).show();
                    loadMyBookings(); // Reload list
                } else {
                    Toast.makeText(MyBookingsActivity.this, "Không thể hủy vé", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Cancel failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyBookingsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Cancel error", t);
            }
        });
    }
}

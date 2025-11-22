package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(new ArrayList<>(), this::onCancelBooking);
        rvBookings.setAdapter(bookingAdapter);

        // Open booking details when a ticket is clicked
        bookingAdapter.setOnItemClickListener(booking -> {
            Intent intent = new Intent(MyBookingsActivity.this, BookingDetailActivity.class);
            intent.putExtra("booking_id", booking.getId());
            startActivity(intent);
        });

        loadMyBookings();

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_tickets);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, vn.hcmute.busbooking.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_tickets) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                Intent intent = new Intent(this, FavoritesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_account) {
                Intent intent = new Intent(this, UserAccountActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });

        // Handle system back with dispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(MyBookingsActivity.this, vn.hcmute.busbooking.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh bookings when returning from detail screen
        Log.d(TAG, "onResume: Refreshing bookings list");
        loadMyBookings();
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
            String message = "pending_refund".equals(status)
                ? "Vé đang được xử lý hoàn tiền, không thể hủy"
                : "Không thể hủy vé này";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.AdminOfflinePaymentAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class AdminOfflinePaymentActivity extends AppCompatActivity {

    private static final String TAG = "AdminOfflinePayment";

    private RecyclerView rvPendingPayments;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;

    private AdminOfflinePaymentAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private List<Map<String, Object>> pendingPayments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_offline_payment);

        // Setup window insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            WindowInsetsCompat windowInsets = insets;
            insets.getInsets(WindowInsetsCompat.Type.systemBars());
            return WindowInsetsCompat.CONSUMED;
        });

        initializeViews();
        setupToolbar();
        loadPendingPayments();
    }

    private void initializeViews() {
        rvPendingPayments = findViewById(R.id.rvPendingPayments);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        rvPendingPayments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminOfflinePaymentAdapter(pendingPayments, booking -> confirmPayment(booking));
        rvPendingPayments.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Xác nhận thanh toán offline");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPendingPayments() {
        setLoadingState(true);
        int userId = sessionManager.getUserId();

        apiService.getPendingOfflinePayments(userId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    pendingPayments.clear();
                    pendingPayments.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (pendingPayments.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvPendingPayments.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvPendingPayments.setVisibility(View.VISIBLE);
                    }

                    Log.d(TAG, "Loaded " + pendingPayments.size() + " pending payments");
                } else {
                    Log.e(TAG, "Failed to load pending payments: " + response.code());
                    tvEmptyState.setText("Lỗi khi tải dữ liệu");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(AdminOfflinePaymentActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmPayment(Map<String, Object> booking) {
        int bookingId = ((Number) booking.get("id")).intValue();
        int userId = sessionManager.getUserId();

        setLoadingState(true);
        apiService.adminConfirmOfflinePayment(userId, bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoadingState(false);

                if (response.isSuccessful()) {
                    Log.d(TAG, "Payment confirmed: " + bookingId);
                    Toast.makeText(AdminOfflinePaymentActivity.this, "Xác nhận thành công", Toast.LENGTH_SHORT).show();

                    // Remove confirmed booking from list
                    pendingPayments.removeIf(b -> ((Number) b.get("id")).intValue() == bookingId);
                    adapter.notifyDataSetChanged();

                    if (pendingPayments.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvPendingPayments.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Failed to confirm payment: " + response.code());
                    Toast.makeText(AdminOfflinePaymentActivity.this, "Xác nhận thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(AdminOfflinePaymentActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}


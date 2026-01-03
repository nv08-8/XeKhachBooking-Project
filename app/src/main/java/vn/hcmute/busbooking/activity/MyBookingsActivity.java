package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.guest.GuestAccountActivity;
import vn.hcmute.busbooking.adapter.BookingAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.utils.ReviewStore;
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.ws.SocketManager;

public class MyBookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private BookingAdapter bookingAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private ReviewStore reviewStore;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private View statusBarScrim;
    private BottomNavigationView bottomNav;

    private List<Booking> allBookings = new ArrayList<>();
    // Cached lists to display per tab
    private List<Booking> listCurrent = new ArrayList<>();
    private List<Booking> listCancelled = new ArrayList<>();
    private List<Booking> listPendingReview = new ArrayList<>();
    private List<Booking> listReviewed = new ArrayList<>();

    // Pending countdowns (bookingId -> remainingMillis)
    private final java.util.Map<Integer, Long> pendingCountdowns = new java.util.HashMap<>();
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            boolean changed = computePendingCountdowns();
            if (changed) {
                // update adapter with new countdowns
                runOnUiThread(() -> {
                    try { bookingAdapter.updatePendingCountdowns(pendingCountdowns); } catch (Exception ignored) {}
                });
            }
            // schedule next tick
            countdownHandler.postDelayed(this, 1000);
        }
    };

    // Polling handler
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final long POLL_INTERVAL_MS = 60 * 1000L; // 60 seconds
    private boolean loadingInProgress = false;
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loadingInProgress) {
                loadMyBookings();
            }
            // schedule next
            pollingHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private SocketManager.BookingEventListener socketListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_my_bookings);

        appBarLayout = findViewById(R.id.appBarLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        handleWindowInsets();

        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);

        sessionManager = new SessionManager(this);
        reviewStore = new ReviewStore(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();
        setupTabs();
        // Mặc định chọn tab đầu tiên: "Hiện tại"
        if (tabLayout.getTabCount() > 0) {
            TabLayout.Tab defaultTab = tabLayout.getTabAt(0);
            if (defaultTab != null) defaultTab.select();
        }
        setupBottomNavigation();
        loadMyBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Always refresh bookings when returning to this activity
        Log.d("MyBookings", "onResume: Refreshing bookings list");
        loadMyBookings();

        // Start polling when activity visible
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL_MS);

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tickets);
        }

        // Register a global socket listener to show banner when booking expired
        socketListener = payload -> {
            try {
                Snackbar.make(findViewById(android.R.id.content), "Một vé chờ thanh toán đã hết hạn", Snackbar.LENGTH_LONG).show();
            } catch (Exception ignored) {}
            refreshBookings();
        };
        SocketManager.getInstance(this).addBookingListener(socketListener);
        startCountdownTicker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollingHandler.removeCallbacks(pollingRunnable);
        if (socketListener != null) {
            SocketManager.getInstance(this).removeBookingListener(socketListener);
            socketListener = null;
        }
        stopCountdownTicker();
    }

    private void handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = statusBarHeight;
            v.setLayoutParams(params);
            if (statusBarScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                scrimParams.height = statusBarHeight;
                statusBarScrim.setLayoutParams(scrimParams);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });
    }

    private void setupRecyclerView() {
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(new ArrayList<>(), this);
        rvBookings.setAdapter(bookingAdapter);

        bookingAdapter.setOnBookingClickListener(booking -> {
            Intent intent = new Intent(MyBookingsActivity.this, BookingDetailActivity.class);
            intent.putExtra("booking_id", booking.getId());
            startActivity(intent);
        });
    }

    private void setupTabs() {
        // Updated tabs to include "Chờ nhận xét" and "Đã nhận xét"
        tabLayout.addTab(tabLayout.newTab().setText("Hiện tại"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ nhận xét"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã nhận xét"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã hủy"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
             @Override
             public void onTabSelected(TabLayout.Tab tab) {
                showListForTab(tab.getPosition());
             }
             @Override
             public void onTabUnselected(TabLayout.Tab tab) { }
             @Override
             public void onTabReselected(TabLayout.Tab tab) { }
         });
     }

     private void computeLists(List<Booking> bookings) {
        listCurrent.clear(); listCancelled.clear();
        listPendingReview.clear(); listReviewed.clear();

        long nowMillis = System.currentTimeMillis();

        for (Booking booking : bookings) {
            if (booking == null) continue;
            String status = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase().trim();
            
            long arrivalTime = parseDateToMillis(booking.getArrival_time());
            if (arrivalTime <= 0) {
                long departureTime = parseDateToMillis(booking.getDeparture_time());
                if (departureTime > 0) arrivalTime = departureTime + (4 * 60 * 60 * 1000L); 
            }

            boolean isCancelled = status.equals("cancelled") || status.equals("canceled") || status.equals("expired") || status.contains("hủy") || status.contains("huy");
            boolean isPending = status.equals("pending");
            boolean isPast = (arrivalTime > 0 && nowMillis > arrivalTime);
            boolean isConfirmed = !isCancelled && !isPending;
            
            // 1. Tab "Hiện tại"
            if (isPending || (isConfirmed && !isPast)) {
                listCurrent.add(booking);
            }

            // 2. Tab "Đã hủy"
            if (isCancelled || (isPending && isPast)) {
                listCancelled.add(booking);
            }

            // 3. Feedback categories (Part of "Đã đi")
            if (isConfirmed && isPast) {
                if (reviewStore.hasLocalReview(booking.getId())) {
                    listReviewed.add(booking);
                } else {
                    listPendingReview.add(booking);
                }
            }
         }

        Comparator<Booking> descByDeparture = (a, b) -> Long.compare(parseDateToMillis(b.getDeparture_time()), parseDateToMillis(a.getDeparture_time()));
        
        try {
            Collections.sort(listCurrent, descByDeparture);
            Collections.sort(listPendingReview, descByDeparture);
            Collections.sort(listReviewed, descByDeparture);
            Collections.sort(listCancelled, descByDeparture);
        } catch (Exception ignored) {}

        computePendingCountdowns();
        try { bookingAdapter.updatePendingCountdowns(pendingCountdowns); } catch (Exception ignored) {}
     }

    private void showListForTab(int tabPosition) {
        if (tabPosition < 0) tabPosition = 0;
        List<Booking> toShow;
        switch (tabPosition) {
            case 0: toShow = listCurrent; break;
            case 1: toShow = listPendingReview; break;
            case 2: toShow = listReviewed; break;
            case 3: toShow = listCancelled; break;
            default: toShow = listCurrent; break;
        }
        bookingAdapter.updateBookings(new ArrayList<>(toShow));
        if (toShow.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
        }
    }

     private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_tickets);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
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
                Intent intent;
                if (sessionManager.isLoggedIn()) {
                    intent = new Intent(this, UserAccountActivity.class);
                } else {
                    intent = new Intent(this, GuestAccountActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
    }

    private void loadMyBookings() {
        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (loadingInProgress) return;
        loadingInProgress = true;

        progressBar.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        Set<Integer> prevPendingIds = new HashSet<>();
        for (Booking b : allBookings) {
            if (b.getStatus() != null && b.getStatus().equals("pending")) prevPendingIds.add(b.getId());
        }

        apiService.getMyBookings(userId).enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> response) {
                loadingInProgress = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Booking> newList = response.body();

                    Set<Integer> currPendingIds = new HashSet<>();
                    for (Booking b : newList) {
                        if (b.getStatus() != null && b.getStatus().equals("pending")) currPendingIds.add(b.getId());
                    }

                    Set<Integer> expiredDetected = new HashSet<>(prevPendingIds);
                    expiredDetected.removeAll(currPendingIds);
                    if (!expiredDetected.isEmpty()) {
                        String msg = expiredDetected.size() == 1
                                ? "Một vé chờ thanh toán đã hết hạn và bị chuyển sang Hết hạn"
                                : expiredDetected.size() + " vé chờ thanh toán đã hết hạn và bị chuyển sang Hết hạn";
                        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
                    }

                    allBookings = newList;
                    computeLists(allBookings);
                    int sel = tabLayout.getSelectedTabPosition();
                    if (sel < 0) sel = 0;
                    showListForTab(sel);
                 } else {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Không thể tải danh sách vé");
                }
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                loadingInProgress = false;
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private long parseDateToMillis(String dateStr) {
        if (dateStr == null) return -1;
        String[] formats = new String[] {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                Date d = sdf.parse(dateStr);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {}
        }
        return -1;
    }

    public void refreshBookings() {
        runOnUiThread(this::loadMyBookings);
    }

    private void startCountdownTicker() {
        countdownHandler.removeCallbacks(countdownRunnable);
        countdownHandler.post(countdownRunnable);
    }

    private void stopCountdownTicker() {
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    private boolean computePendingCountdowns() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        long ttl = 10 * 60 * 1000L;
        java.util.Set<Integer> toCancel = new java.util.HashSet<>();

        for (Booking b : allBookings) {
            if (b == null || b.getStatus() == null || !b.getStatus().equalsIgnoreCase("pending")) continue;

            String paymentMethod = b.getPayment_method();
            boolean isOfflinePayment = paymentMethod != null &&
                (paymentMethod.toLowerCase().contains("cash") ||
                 paymentMethod.toLowerCase().contains("offline") ||
                 paymentMethod.toLowerCase().contains("cod") ||
                 paymentMethod.toLowerCase().contains("counter"));

            if (isOfflinePayment) continue;

            int id = b.getId();
            long created = parseDateToMillis(b.getCreated_at());
            if (created <= 0) continue;
            long rem = (created + ttl) - now;
            Long prev = pendingCountdowns.get(id);
            if (rem <= 0) {
                toCancel.add(id);
                if (prev != null) {
                    pendingCountdowns.remove(id);
                    changed = true;
                }
            } else {
                if (prev == null || !prev.equals(rem)) {
                    pendingCountdowns.put(id, rem);
                    changed = true;
                }
            }
        }

        if (!toCancel.isEmpty()) {
            for (Integer bid : toCancel) {
                cancelBookingOnServer(bid);
            }
        }
        return changed;
    }

    private void cancelBookingOnServer(int bookingId) {
        apiService.cancelBooking(bookingId).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> loadMyBookings());
                } else {
                    Log.e("MyBookings", "Failed to auto-cancel booking " + bookingId);
                    pendingCountdowns.remove(bookingId);
                    runOnUiThread(() -> {
                        try { bookingAdapter.updatePendingCountdowns(pendingCountdowns); } catch (Exception ignored) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                Log.e("MyBookings", "Network error while auto-cancelling booking " + bookingId, t);
            }
        });
    }
}

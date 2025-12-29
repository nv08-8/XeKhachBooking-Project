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
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.ws.SocketManager;

public class MyBookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private BookingAdapter bookingAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private View statusBarScrim;
    private BottomNavigationView bottomNav;

    private List<Booking> allBookings = new ArrayList<>();
    // Cached lists to display per tab
    private List<Booking> listCurrent = new ArrayList<>();
    private List<Booking> listPast = new ArrayList<>();
    private List<Booking> listCancelled = new ArrayList<>();
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
        // This ensures newly created offline bookings are displayed immediately
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
            // payload contains booking info; show banner and refresh
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
        // Stop polling to avoid unnecessary work when activity not visible
        pollingHandler.removeCallbacks(pollingRunnable);

        // Unregister socket listener
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
        bookingAdapter = new BookingAdapter(new ArrayList<>());
        rvBookings.setAdapter(bookingAdapter);

        // ✅ Add click listener to open BookingDetailActivity
        bookingAdapter.setOnBookingClickListener(booking -> {
            Intent intent = new Intent(MyBookingsActivity.this, BookingDetailActivity.class);
            intent.putExtra("booking_id", booking.getId());
            startActivity(intent);
        });
    }

    private void setupTabs() {
        // Display only 3 tabs as requested: Hiện tại (3 months), Đã đi (past), Đã hủy
        tabLayout.addTab(tabLayout.newTab().setText("Hiện tại"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã đi"));
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
         listCurrent.clear(); listPast.clear(); listCancelled.clear();

        long nowMillis = System.currentTimeMillis();

        // 3 months ago limit
        Calendar threeMonthsAgo = Calendar.getInstance();
        threeMonthsAgo.add(Calendar.MONTH, -3);
        long threeMonthsAgoMillis = threeMonthsAgo.getTimeInMillis();
        
        for (Booking booking : bookings) {
            if (booking == null) continue;
            String status = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase().trim();
            
            // Xác định thời điểm arrival_time
            long arrivalTime = parseDateToMillis(booking.getArrival_time());
            if (arrivalTime <= 0) {
                long departureTime = parseDateToMillis(booking.getDeparture_time());
                if (departureTime > 0) arrivalTime = departureTime + (4 * 60 * 60 * 1000L); 
            }

            boolean isCancelled = status.equals("cancelled") || status.equals("canceled") || status.equals("expired") || status.contains("hủy") || status.contains("huy");
            
            // --- LOGIC PHÂN LOẠI ---

            // 1. Tab "Hiện tại": Hiển thị TẤT CẢ các vé trong 3 tháng gần nhất (bất kể trạng thái)
            long sortTime = parseDateToMillis(booking.getDeparture_time());
            if (sortTime == -1) sortTime = parseDateToMillis(booking.getCreated_at());

            if (sortTime == -1 || sortTime >= threeMonthsAgoMillis) {
                listCurrent.add(booking);
            }

            // 2. Tab "Đã hủy": Vé status hủy/hết hạn HOẶC pending mà quá giờ đến
            if (isCancelled) {
                listCancelled.add(booking);
            } else if (status.equals("pending")) {
                // Pending
                // Nếu đã qua arrival_time -> coi như hủy (người dùng không đi được nữa)
                if (arrivalTime > 0 && nowMillis > arrivalTime) {
                     listCancelled.add(booking);
                }
            }

            // 3. Tab "Đã đi": Vé Confirmed/Completed và ĐÃ qua giờ đến
            if (!isCancelled && !status.equals("pending")) {
                // Confirmed / Completed / Or others
                // Nếu đã qua arrival_time -> Coi như đã đi -> Tab Đã đi
                if (arrivalTime > 0 && nowMillis > arrivalTime) {
                    listPast.add(booking);
                }
            }
         }

        // Sort lists
        // Hiện tại: Mới nhất lên đầu (Descending)
        Comparator<Booking> descByDeparture = (a, b) -> Long.compare(parseDateToMillis(b.getDeparture_time()), parseDateToMillis(a.getDeparture_time()));
        
        try {
            Collections.sort(listCurrent, descByDeparture);
        } catch (Exception ignored) {}
        try {
            Collections.sort(listPast, descByDeparture);
        } catch (Exception ignored) {}
        try {
            Collections.sort(listCancelled, descByDeparture);
        } catch (Exception ignored) {}

        // Recompute pending countdowns after building lists
        computePendingCountdowns();
        try { bookingAdapter.updatePendingCountdowns(pendingCountdowns); } catch (Exception ignored) {}
     }

    private void showListForTab(int tabPosition) {
        if (tabPosition < 0) tabPosition = 0;
        List<Booking> toShow;
        switch (tabPosition) {
            case 0: toShow = listCurrent; break;
            case 1: toShow = listPast; break;
            case 2: toShow = listCancelled; break;
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

        // Prevent overlapping requests
        if (loadingInProgress) return;
        loadingInProgress = true;

        progressBar.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        // Capture previous pending booking ids so we can detect expired ones after refresh
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

                    // Build set of current pending ids
                    Set<Integer> currPendingIds = new HashSet<>();
                    for (Booking b : newList) {
                        if (b.getStatus() != null && b.getStatus().equals("pending")) currPendingIds.add(b.getId());
                    }

                    // Determine which pending bookings expired (present before, missing now)
                    Set<Integer> expiredDetected = new HashSet<>(prevPendingIds);
                    expiredDetected.removeAll(currPendingIds); // expiredDetected now contains expired ids
                    if (!expiredDetected.isEmpty()) {
                        // Show a Snackbar informing user
                        String msg = expiredDetected.size() == 1
                                ? "Một vé chờ thanh toán đã hết hạn và bị chuyển sang Hết hạn"
                                : expiredDetected.size() + " vé chờ thanh toán đã hết hạn và bị chuyển sang Hết hạn";
                        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
                    }

                    allBookings = newList;
                    // Compute and cache lists, then show the current tab
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

    // Parse multiple possible date formats and return milliseconds since epoch, or -1 if invalid
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

    // Allow external callers (e.g., WebSocket client) to trigger a refresh
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

    // Returns true if pendingCountdowns changed (for UI update)
    private boolean computePendingCountdowns() {
        boolean changed = false;
        long now = System.currentTimeMillis();

        // TTL = 10 minutes (in ms)
        long ttl = 10 * 60 * 1000L;

        java.util.Set<Integer> toCancel = new java.util.HashSet<>();

        for (Booking b : allBookings) {
            if (b == null) continue;
            if (b.getStatus() == null) continue;
            if (!b.getStatus().equalsIgnoreCase("pending")) continue;

            // ✅ Skip offline payment bookings - they should NOT be auto-cancelled after 10 minutes
            String paymentMethod = b.getPayment_method();
            boolean isOfflinePayment = paymentMethod != null &&
                (paymentMethod.toLowerCase().contains("cash") ||
                 paymentMethod.toLowerCase().contains("offline") ||
                 paymentMethod.toLowerCase().contains("cod") ||
                 paymentMethod.toLowerCase().contains("counter"));

            if (isOfflinePayment) {
                // Offline payment booking - skip countdown and auto-cancel
                continue;
            }

            int id = b.getId();
            long created = parseDateToMillis(b.getCreated_at());
            if (created <= 0) {
                // If created_at missing, skip countdown
                continue;
            }
            long rem = (created + ttl) - now;
            Long prev = pendingCountdowns.get(id);
            if (rem <= 0) {
                // expired -> cancel booking via API (ONLY for online payment)
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
            // Call cancel API for each expired pending booking
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
                    // On success, refresh the entire list to show the change
                    runOnUiThread(() -> loadMyBookings());
                } else {
                    // On failure (e.g., server rejects cancellation), log it.
                    Log.e("MyBookings", "Failed to auto-cancel booking " + bookingId + ". Server response: " + response.code());

                    // To prevent trying to cancel again every second, remove it from the countdown map.
                    // This stops the countdown ticker from re-triggering cancellation for this booking.
                    pendingCountdowns.remove(bookingId);

                    // We should also trigger a refresh of the adapter to update the UI for this specific item,
                    // so the countdown disappears.
                    runOnUiThread(() -> {
                        try {
                            bookingAdapter.updatePendingCountdowns(pendingCountdowns);
                        } catch (Exception ignored) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                // On network failure, just log the error. Don't refresh.
                Log.e("MyBookings", "Network error while auto-cancelling booking " + bookingId, t);
            }
        });
    }
}

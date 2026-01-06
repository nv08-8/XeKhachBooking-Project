package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.guest.GuestAccountActivity;
import vn.hcmute.busbooking.adapter.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";
    private SessionManager sessionManager;
    private RecyclerView rvFavorites;
    private TextView tvFavoritesEmpty;
    private TripAdapter tripAdapter;
    private AppBarLayout appBarLayout;
    private View statusBarScrim;
    private BottomNavigationView bottomNav;
    private LinearLayout dateTabsContainer;
    private HorizontalScrollView dateScrollView;
    private ApiService apiService;

    private List<Trip> allFavoriteTrips = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();
    private List<View> dateTabViews = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_favorites);

        appBarLayout = findViewById(R.id.appBarLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        handleWindowInsets();

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        dateScrollView = findViewById(R.id.dateScrollView);
        dateTabsContainer = findViewById(R.id.dateTabsContainer);
        setupDateTabs();

        rvFavorites = findViewById(R.id.rvFavorites);
        if (rvFavorites != null) {
            rvFavorites.setLayoutManager(new LinearLayoutManager(this));
            rvFavorites.setHasFixedSize(true);
            tripAdapter = new TripAdapter(this, new ArrayList<>(), new ArrayList<>());
            tripAdapter.setOnItemClickListener(trip -> {
                Intent intent = new Intent(FavoritesActivity.this, TripDetailActivity.class);
                intent.putExtra("trip_id", trip.getId()); // Pass trip ID instead of the whole object
                startActivity(intent);
            });
            rvFavorites.setAdapter(tripAdapter);
        }
        tvFavoritesEmpty = findViewById(R.id.tvFavoritesEmpty);

        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_favorites);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_tickets) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_favorites) {
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

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteTrips();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_favorites);
        }
    }

    private void loadFavoriteTrips() {
        Log.d(TAG, "loadFavoriteTrips() called");

        Integer userId = sessionManager.getUserId();
        Log.d(TAG, "User ID: " + userId);

        if (userId == null) { // User not logged in
            Log.d(TAG, "User not logged in");
            rvFavorites.setVisibility(View.GONE);
            tvFavoritesEmpty.setText("Vui lòng đăng nhập để xem các chuyến đi yêu thích");
            tvFavoritesEmpty.setVisibility(View.VISIBLE);
            tripAdapter.updateTrips(new ArrayList<>());
            return;
        }

        apiService.getFavorites(userId).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allFavoriteTrips = response.body();
                    List<Integer> favoriteIds = allFavoriteTrips.stream().map(Trip::getId).collect(Collectors.toList());
                    tripAdapter = new TripAdapter(FavoritesActivity.this, allFavoriteTrips, favoriteIds);
                    rvFavorites.setAdapter(tripAdapter);
                    Log.d(TAG, "Loaded " + allFavoriteTrips.size() + " favorite trips from API");
                    filterTripsByDate();
                } else {
                    Toast.makeText(FavoritesActivity.this, "Không thể tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Log.e(TAG, "Failed to load favorites from API", t);
                Toast.makeText(FavoritesActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDateTabs() {
        dateTabsContainer.removeAllViews();
        dateTabViews.clear();

        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Calendar date = (Calendar) today.clone();
            date.add(Calendar.DAY_OF_YEAR, i);

            View tabView = createDateTab(date, i == 0);
            dateTabsContainer.addView(tabView);
            dateTabViews.add(tabView);
        }

        View chooseDateView = createChooseDateTab();
        dateTabsContainer.addView(chooseDateView);
    }

    private View createDateTab(Calendar date, boolean isSelected) {
        LinearLayout tabLayout = new LinearLayout(this);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabParams.setMarginEnd(dpToPx(16));
        tabLayout.setLayoutParams(tabParams);
        tabLayout.setOrientation(LinearLayout.VERTICAL);
        tabLayout.setGravity(Gravity.CENTER);
        tabLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        tabLayout.setMinimumWidth(dpToPx(80));

        TextView tvDayLabel = new TextView(this);
        tvDayLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvDayLabel.setTextColor(isSelected ?
            ContextCompat.getColor(this, R.color.textPrimary) :
            ContextCompat.getColor(this, R.color.textSecondary));
        tvDayLabel.setText(getDayLabel(date));
        tvDayLabel.setGravity(Gravity.CENTER);
        tvDayLabel.setSingleLine(false);
        tvDayLabel.setMaxLines(1);
        tvDayLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (isSelected) {
            tvDayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvDayLabel.setLayoutParams(labelParams);
        tabLayout.addView(tvDayLabel);

        TextView tvDate = new TextView(this);
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvDate.setTextColor(isSelected ?
            ContextCompat.getColor(this, R.color.textPrimary) :
            ContextCompat.getColor(this, R.color.textSecondary));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        tvDate.setText(dateFormat.format(date.getTime()));
        tvDate.setGravity(Gravity.CENTER);
        if (isSelected) {
            tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        tvDate.setLayoutParams(dateParams);
        tabLayout.addView(tvDate);

        View underline = new View(this);
        LinearLayout.LayoutParams underlineParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(3)
        );
        underline.setLayoutParams(underlineParams);
        underline.setBackgroundColor(isSelected ?
            ContextCompat.getColor(this, R.color.colorPrimary) :
            android.graphics.Color.TRANSPARENT);
        tabLayout.addView(underline);

        Calendar finalDate = (Calendar) date.clone();
        tabLayout.setOnClickListener(v -> {
            selectedDate = finalDate;
            updateSelectedDateTab();
            filterTripsByDate();
        });

        tabLayout.setTag(R.id.tag_underline, underline);
        tabLayout.setTag(R.id.tag_day_label, tvDayLabel);
        tabLayout.setTag(R.id.tag_date_text, tvDate);

        return tabLayout;
    }

    private View createChooseDateTab() {
        LinearLayout tabLayout = new LinearLayout(this);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabLayout.setLayoutParams(tabParams);
        tabLayout.setOrientation(LinearLayout.VERTICAL);
        tabLayout.setGravity(Gravity.CENTER);
        tabLayout.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

        TextView tvIcon = new TextView(this);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tvIcon.setText("📅");
        tvIcon.setGravity(Gravity.CENTER);
        tabLayout.addView(tvIcon);

        TextView tvLabel = new TextView(this);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        tvLabel.setText("Chọn ngày");
        tvLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(0, dpToPx(4), 0, 0);
        tvLabel.setLayoutParams(labelParams);
        tabLayout.addView(tvLabel);

        tabLayout.setOnClickListener(v -> showDatePicker());

        return tabLayout;
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                setupDateTabs();
                filterTripsByDate();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(today.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateSelectedDateTab() {
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < Math.min(dateTabViews.size(), 7); i++) {
            Calendar tabDate = (Calendar) today.clone();
            tabDate.add(Calendar.DAY_OF_YEAR, i);

            boolean isSelected = isSameDay(tabDate, selectedDate);
            View tabView = dateTabViews.get(i);

            TextView tvDayLabel = (TextView) tabView.getTag(R.id.tag_day_label);
            TextView tvDate = (TextView) tabView.getTag(R.id.tag_date_text);
            View underline = (View) tabView.getTag(R.id.tag_underline);

            if (tvDayLabel != null && tvDate != null && underline != null) {
                tvDayLabel.setTextColor(isSelected ?
                    ContextCompat.getColor(this, R.color.textPrimary) :
                    ContextCompat.getColor(this, R.color.textSecondary));
                tvDate.setTextColor(isSelected ?
                    ContextCompat.getColor(this, R.color.textPrimary) :
                    ContextCompat.getColor(this, R.color.textSecondary));

                tvDayLabel.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                tvDate.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

                underline.setBackgroundColor(isSelected ?
                    ContextCompat.getColor(this, R.color.colorPrimary) :
                    android.graphics.Color.TRANSPARENT);
            }
        }
    }

    private void filterTripsByDate() {
        List<Trip> filteredTrips = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = dateFormat.format(selectedDate.getTime());

        for (Trip trip : allFavoriteTrips) {
            String departureTime = trip.getDepartureTime();
            if (departureTime != null && departureTime.length() >= 10) {
                String tripDateStr = departureTime.substring(0, 10);
                if (tripDateStr.equals(selectedDateStr)) {
                    filteredTrips.add(trip);
                }
            }
        }

        Log.d(TAG, "Filtered " + filteredTrips.size() + " trips for date: " + selectedDateStr);

        if (filteredTrips.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            tvFavoritesEmpty.setText("Không có chuyến đi yêu thích nào trong ngày này");
            tvFavoritesEmpty.setVisibility(View.VISIBLE);
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            tvFavoritesEmpty.setVisibility(View.GONE);
            tripAdapter.updateTrips(filteredTrips);
        }
    }

    private String getDayLabel(Calendar date) {
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (isSameDay(date, today)) {
            return "Hôm nay";
        } else if (isSameDay(date, tomorrow)) {
            return "Ngày mai";
        } else {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("vi", "VN"));
            return dayFormat.format(date.getTime());
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            getResources().getDisplayMetrics()
        );
    }
}

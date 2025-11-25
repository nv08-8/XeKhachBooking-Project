package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.ApplicationInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;

public class TripListActivity extends AppCompatActivity {

    private static final String TAG = "TripListActivity";

    private TextView tvRoute, tvDate, tvEmptyState;
    private Spinner spinnerPriceFilter, spinnerSeatTypeFilter, spinnerTimeFilter;
    private RecyclerView rvTrips;
    private TripAdapter tripAdapter;
    private List<Trip> allTrips = new ArrayList<>();
    private ApiService apiService;

    private boolean isReturn;
    private String origin, destination, travelDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_trip_list);

        View mainView = findViewById(R.id.main);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            appBarLayout.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // Setup toolbar back navigation
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Use dispatcher for modern back handling (equivalent to finish())
                getOnBackPressedDispatcher().onBackPressed();
            });
        }

        // Initialize views
        tvRoute = findViewById(R.id.tvRoute);
        tvDate = findViewById(R.id.tvDate);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        spinnerPriceFilter = findViewById(R.id.spinnerPriceFilter);
        spinnerSeatTypeFilter = findViewById(R.id.spinnerSeatTypeFilter);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);
        rvTrips = findViewById(R.id.rvTrips);

        // Get data from intent
        origin = getIntent().getStringExtra("origin");
        destination = getIntent().getStringExtra("destination");
        travelDate = getIntent().getStringExtra("date");
        isReturn = getIntent().getBooleanExtra("isReturn", false);

        if (origin == null) origin = "";
        if (destination == null) destination = "";
        if (travelDate == null) travelDate = "Hôm nay";

        tvRoute.setText(origin + " → " + destination);
        tvDate.setText("Ngày: " + travelDate);

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(this, new ArrayList<>());
        rvTrips.setAdapter(tripAdapter);

        tripAdapter.setOnItemClickListener(trip -> {
            // Open TripDetailActivity first to show trip details
            Intent intent = new Intent(TripListActivity.this, vn.hcmute.busbooking.TripDetailActivity.class);
            intent.putExtra("trip", trip);
            intent.putExtra("isReturn", isReturn);
            intent.putExtra("returnOrigin", destination);
            intent.putExtra("returnDestination", origin);
            intent.putExtra("returnDate", travelDate);
            startActivity(intent);
        });

        apiService = ApiClient.getClient().create(ApiService.class);
        Log.d(TAG, "Initial search params: origin='" + origin + "', destination='" + destination + "', date='" + travelDate + "'");
        Toast.makeText(this, "Đang tìm chuyến...", Toast.LENGTH_SHORT).show();
        // runtime debug check instead of BuildConfig.DEBUG (avoids import issues)
        final boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        fetchTrips(origin, destination, travelDate, isDebug);

        setupFilters();
    }

    private void fetchTrips(String from, String to, String date, boolean isDebug) {
        String apiDate = "";
        if (!"Hôm nay".equals(date)) {
            try {
                SimpleDateFormat fromUser = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat forApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsedDate = fromUser.parse(date);
                if (parsedDate != null) {
                    apiDate = forApi.format(parsedDate);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Could not parse date: " + date, e);
            }
        }

        final String apiDateFinal = apiDate;
        apiService.getTrips(from, to, apiDateFinal).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTrips = response.body();
                    applyFilters();
                    Log.d(TAG, "Trips loaded: " + allTrips.size());
                    if (allTrips.isEmpty()) {
                        Toast.makeText(TripListActivity.this, "Không tìm thấy chuyến khớp. Thử mở rộng tìm kiếm hoặc kiểm tra kết nối.", Toast.LENGTH_LONG).show();
                        // Debug fallback: if in debug, try fetch without origin/destination to see if server returns anything
                        if (isDebug) {
                            Log.d(TAG, "DEBUG: performing fallback fetch (no origin/destination)");
                            apiService.getTrips("", "", apiDateFinal).enqueue(new Callback<List<Trip>>() {
                                @Override
                                public void onResponse(Call<List<Trip>> call2, Response<List<Trip>> response2) {
                                    if (response2.isSuccessful() && response2.body() != null && !response2.body().isEmpty()) {
                                        Log.d(TAG, "DEBUG fallback found trips: " + response2.body().size());
                                    } else {
                                        String bodyStr = "";
                                        try { if (response2.errorBody() != null) bodyStr = response2.errorBody().string(); } catch (Exception ignored) {}
                                        Log.w(TAG, "DEBUG fallback no data; code=" + response2.code() + " body=" + bodyStr);
                                    }
                                }

                                @Override
                                public void onFailure(Call<List<Trip>> call2, Throwable t2) {
                                    Log.e(TAG, "DEBUG fallback failed: " + t2.getMessage(), t2);
                                }
                            });
                        }
                    }
                } else {
                    Log.e(TAG, "Response not successful. Code: " + response.code());
                    String err = "";
                    try { if (response.errorBody() != null) err = response.errorBody().string(); } catch (Exception ignored) {}
                    Toast.makeText(TripListActivity.this, "Lỗi server: " + response.code() + (err.isEmpty() ? "" : (" - " + err)), Toast.LENGTH_LONG).show();
                    allTrips.clear();
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(TripListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                allTrips.clear();
                applyFilters();
            }
        });
    }

    private void setupFilters() {
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerPriceFilter.setOnItemSelectedListener(filterListener);
        spinnerSeatTypeFilter.setOnItemSelectedListener(filterListener);
        spinnerTimeFilter.setOnItemSelectedListener(filterListener);
    }

    private void applyFilters() {
        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : allTrips) {
            if (checkPriceFilter(trip) && checkSeatTypeFilter(trip) && checkTimeFilter(trip)) {
                filteredTrips.add(trip);
            }
        }
        tripAdapter.updateTrips(filteredTrips);

        // Show/hide empty state
        if (filteredTrips.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTrips.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTrips.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkPriceFilter(Trip trip) {
        int selectedPosition = spinnerPriceFilter.getSelectedItemPosition();
        double price = trip.getPrice();

        switch (selectedPosition) {
            case 0: return true;
            case 1: return price < 100000;
            case 2: return price >= 100000 && price <= 200000;
            case 3: return price > 200000;
            default: return true;
        }
    }

    private boolean checkSeatTypeFilter(Trip trip) {
        return true;
    }

    private boolean checkTimeFilter(Trip trip) {
        String selectedFilter = spinnerTimeFilter.getSelectedItem().toString();
        if (selectedFilter.contains("Giờ đi")) return true;

        String departureTime = trip.getDepartureTime();
        if (departureTime == null) return false;

        try {
            String iso = departureTime;
            int hour = Integer.parseInt(iso.substring(11, 13));

            if (selectedFilter.contains("Sáng")) return hour >= 6 && hour < 12;
            if (selectedFilter.contains("Chiều")) return hour >= 12 && hour < 18;
            if (selectedFilter.contains("Tối")) return hour >= 18 && hour < 24;

        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse giờ: " + departureTime);
            return false;
        }

        return true;
    }
}

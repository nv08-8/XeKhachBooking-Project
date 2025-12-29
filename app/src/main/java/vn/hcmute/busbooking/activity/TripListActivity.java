package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.fragment.FilterBottomSheetFragment;
import vn.hcmute.busbooking.fragment.SortBottomSheetFragment;
import vn.hcmute.busbooking.fragment.OperatorsBottomSheetFragment;
import vn.hcmute.busbooking.fragment.TimeBottomSheetFragment;
import vn.hcmute.busbooking.model.Trip;

public class TripListActivity extends AppCompatActivity implements FilterBottomSheetFragment.FilterListener {

    private static final String TAG = "TripListActivity";

    private TextView tvRoute, tvDate, tvEmptyState;
    private RecyclerView rvTrips;
    private TripAdapter tripAdapter;
    private List<Trip> allTrips = new ArrayList<>();
    private ApiService apiService;

    private EditText etSearchTrip;
    private MaterialButton btnSort, btnTime, btnOperator;
    private String searchQuery = "";
    private String selectedOperator = ""; // empty means all
    private String selectedBusType = "";
    private String selectedPickup = "";
    private String selectedDropoff = "";
    private int sortMode = 0; // 0=none,1=priceAsc,2=priceDesc,3=timeAsc,4=timeDesc

    private boolean isReturn;
    private String origin, destination, travelDate;

    private float minPrice = 0, maxPrice = 2000000;
    private float minTime = 0, maxTime = 24;

    private List<String> operatorsCache = null;
    private boolean isFetchingOperators = false;
    private AlertDialog activeDialog = null;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tvRoute = findViewById(R.id.tvRoute);
        tvDate = findViewById(R.id.tvDate);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        TextView tvChange = findViewById(R.id.tvChange);
        rvTrips = findViewById(R.id.rvTrips);

        btnSort = findViewById(R.id.btnSort);
        btnTime = findViewById(R.id.btnTime);
        btnOperator = findViewById(R.id.btnOperator);

        origin = getIntent().getStringExtra("origin");
        destination = getIntent().getStringExtra("destination");
        travelDate = getIntent().getStringExtra("date");
        isReturn = getIntent().getBooleanExtra("isReturn", false);

        if (origin == null) origin = "";
        if (destination == null) destination = "";
        if (travelDate == null) travelDate = "Hôm nay";

        tvRoute.setText(String.format(Locale.getDefault(), "%s → %s", origin, destination));
        tvDate.setText(String.format(Locale.getDefault(), "Ngày: %s", travelDate));

        if (tvChange != null) {
            tvChange.setOnClickListener(v -> {
                // Open MainActivity so user can change origin/destination/date
                Intent intent = new Intent(TripListActivity.this, vn.hcmute.busbooking.MainActivity.class);
                intent.putExtra("origin", origin);
                intent.putExtra("destination", destination);
                intent.putExtra("date", travelDate);
                startActivity(intent);
            });
        }

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(this, new ArrayList<>());
        rvTrips.setAdapter(tripAdapter);

        tripAdapter.setOnItemClickListener(trip -> {
            Intent intent = new Intent(TripListActivity.this, vn.hcmute.busbooking.TripDetailActivity.class);
            intent.putExtra("trip", trip);
            intent.putExtra("isReturn", isReturn);
            intent.putExtra("returnOrigin", destination);
            intent.putExtra("returnDestination", origin);
            intent.putExtra("returnDate", travelDate);
            startActivity(intent);
        });

        apiService = ApiClient.getClient().create(ApiService.class);
        // Only fetch if we have both origin and destination; otherwise show empty state and prompt user
        if (origin == null || origin.trim().isEmpty() || destination == null || destination.trim().isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTrips.setVisibility(View.GONE);
        } else {
            fetchTrips(origin, destination, travelDate);
        }
        // Eager fetch operators list (non-blocking). We'll cache result for operator dialog.
        fetchOperators();

        MaterialButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            Log.d(TAG, "Filter button clicked");
            try { Toast.makeText(TripListActivity.this, "Mở bộ lọc...", Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
             FilterBottomSheetFragment bottomSheet = new FilterBottomSheetFragment();
            // provide operator and bus type lists to bottom sheet
            java.util.List<String> ops = new ArrayList<>();
            if (operatorsCache != null && !operatorsCache.isEmpty()) ops.addAll(operatorsCache);
            else {
                for (Trip t : allTrips) if (t.getOperator() != null && !ops.contains(t.getOperator())) ops.add(t.getOperator());
            }
            // bus types from trips
            java.util.List<String> busTypes = new ArrayList<>();
            for (Trip t : allTrips) if (t.getBusType() != null && !busTypes.contains(t.getBusType())) busTypes.add(t.getBusType());
            // pickup/dropoff as origins/destinations for now
            java.util.List<String> pickups = new ArrayList<>();
            java.util.List<String> dropoffs = new ArrayList<>();
            for (Trip t : allTrips) {
                if (t.getOrigin() != null && !pickups.contains(t.getOrigin())) pickups.add(t.getOrigin());
                if (t.getDestination() != null && !dropoffs.contains(t.getDestination())) dropoffs.add(t.getDestination());
            }
            bottomSheet.setOperators(ops);
            bottomSheet.setBusTypes(busTypes);
            bottomSheet.setPickupLocations(pickups);
            bottomSheet.setDropoffLocations(dropoffs);
            bottomSheet.setFilterListener(this);
            try {
                bottomSheet.show(getSupportFragmentManager(), "filter");
            } catch (Exception e) {
                Log.w(TAG, "Filter bottom sheet show() failed, fallback commitAllowingStateLoss", e);
                try {
                    getSupportFragmentManager().beginTransaction().add(bottomSheet, "filter").commitAllowingStateLoss();
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to add FilterBottomSheetFragment", ex);
                }
            }
        });

        // Search input: live filter
        if (etSearchTrip != null) {
            etSearchTrip.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Sort button: open sort bottom sheet
        if (btnSort != null) {
            updateSortButtonText();
            btnSort.setOnClickListener(v -> {
                SortBottomSheetFragment s = new SortBottomSheetFragment();
                s.setSortListener(mode -> {
                    sortMode = mode == 0 ? 0 : (mode == 4 ? 2 : (mode == 5 ? 1 : (mode == 1 ? 3 : sortMode)));
                    // mapping: items[] = {Mặc định(0), Giờ đi sớm nhất(1), Giờ đi muộn nhất(2), Đánh giá(3), Giá giảm(4), Giá tăng(5)}
                    // our sortMode: 0=none,1=priceAsc,2=priceDesc,3=timeAsc,4=timeDesc
                    // so map: 4->2 (price desc), 5->1 (price asc), 1->3 (time asc), 2->4 (time desc)
                    if (mode == 0) sortMode = 0;
                    else if (mode == 4) sortMode = 2;
                    else if (mode == 5) sortMode = 1;
                    else if (mode == 1) sortMode = 3;
                    else if (mode == 2) sortMode = 4;
                    updateSortButtonText();
                    applyFilters();
                });
                s.show(getSupportFragmentManager(), "sort");
            });
        }

        // Time presets: open time bottom sheet
        if (btnTime != null) {
            btnTime.setOnClickListener(v -> {
                TimeBottomSheetFragment t = new TimeBottomSheetFragment();
                t.setTimeListener((min, max) -> {
                    minTime = min; maxTime = max;
                    applyFilters();
                });
                t.show(getSupportFragmentManager(), "time");
            });
        }

        // Operator filter: open operators bottom sheet
        if (btnOperator != null) {
            btnOperator.setOnClickListener(v -> {
                OperatorsBottomSheetFragment ob = new OperatorsBottomSheetFragment();
                // build operators list: prefer cached, else from trips
                List<String> ops = new ArrayList<>();
                if (operatorsCache != null && !operatorsCache.isEmpty()) ops.addAll(operatorsCache);
                else {
                    for (Trip t : allTrips) if (t.getOperator() != null && !ops.contains(t.getOperator())) ops.add(t.getOperator());
                }
                ob.setOperators(ops);
                ob.setOperatorsListener(op -> {
                    selectedOperator = (op == null) ? "" : op;
                    if (selectedOperator.isEmpty()) btnOperator.setText(getString(R.string.operator)); else btnOperator.setText(selectedOperator);
                    applyFilters();
                });
                ob.show(getSupportFragmentManager(), "operators");
            });
        }
    }

    @Override
    protected void onDestroy() {
        // Ensure any active dialogs are dismissed to avoid WindowLeaked
        try {
            if (activeDialog != null && activeDialog.isShowing() && !isFinishing() && !isDestroyed()) {
                activeDialog.dismiss();
            }
        } catch (Exception e) {
            // ignore
        }
        activeDialog = null;
        super.onDestroy();
    }

    // Normalize a string: remove diacritics, lowercase, trim
    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", ""); // remove diacritical marks
        return n.toLowerCase(Locale.getDefault()).trim();
    }

    private void fetchTrips(String from, String to, String date) {
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

        apiService.getTrips(null, from, to, apiDate).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "API response successful, got " + response.body().size() + " trips");
                        List<Trip> trips = new ArrayList<>();
                        for (Map<String, Object> map : response.body()) {
                            try {
                                Trip trip = new Trip(map);
                                trips.add(trip);
                                Log.d(TAG, "Trip created: id=" + trip.getId() + ", origin=" + trip.getOrigin() + ", dest=" + trip.getDestination());
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating Trip from map: " + map, e);
                            }
                        }
                        allTrips = trips;
                        Log.d(TAG, "Total trips after parsing: " + allTrips.size());
                        applyFilters();
                    } else {
                        Log.w(TAG, "API response not successful: " + response.code());
                        allTrips.clear();
                        applyFilters();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onResponse: " + e.getMessage(), e);
                    allTrips.clear();
                    applyFilters();
                    Toast.makeText(TripListActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "API request failed: " + t.getMessage(), t);
                allTrips.clear();
                applyFilters();
                Toast.makeText(TripListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFilterApplied(float minPrice, float maxPrice, float minTime, float maxTime,
                                String operator, String busType, String pickup, String dropoff) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.selectedOperator = operator == null ? "" : operator;
        this.selectedBusType = busType == null ? "" : busType;
        this.selectedPickup = pickup == null ? "" : pickup;
        this.selectedDropoff = dropoff == null ? "" : dropoff;
        // Update button text for operator filter
        if (btnOperator != null) {
            if (this.selectedOperator == null || this.selectedOperator.isEmpty()) btnOperator.setText(getString(R.string.operator));
            else btnOperator.setText(this.selectedOperator);
        }
        applyFilters();
    }

    private void applyFilters() {
        try {
            if (allTrips == null) {
                Log.w(TAG, "applyFilters: allTrips is null");
                return;
            }

            List<Trip> filteredTrips = new ArrayList<>();
            for (Trip trip : allTrips) {
                if (trip == null) {
                    Log.w(TAG, "Trip is null, skipping");
                    continue;
                }

                // Price filter
                try {
                    if (!(trip.getPrice() >= minPrice && trip.getPrice() <= maxPrice)) continue;
                } catch (Exception e) {
                    Log.w(TAG, "Error in price filter: " + e.getMessage());
                    continue;
                }

                // Time filter
                String departure = trip.getDepartureTime();
                double depHour = parseToFractionalHours(departure);
                if (Double.isNaN(depHour)) {
                    Log.w(TAG, "Unparsable departure time: " + departure + " for trip id: " + trip.getId());
                    continue;
                }

                if (!isInTimeRange(depHour, this.minTime, this.maxTime)) continue;

                // Operator filter
                if (selectedOperator != null && !selectedOperator.isEmpty()) {
                    String op = trip.getOperator();
                    if (op == null) continue;
                    String normOp = normalize(op);
                    String normSel = normalize(selectedOperator);
                    if (!normOp.contains(normSel)) continue;
                }

                // Bus type filter
                if (selectedBusType != null && !selectedBusType.isEmpty()) {
                    String bt = trip.getBusType();
                    if (bt == null) continue;
                    if (!normalize(bt).contains(normalize(selectedBusType))) continue;
                }

                // Pickup/dropoff filter
                if (selectedPickup != null && !selectedPickup.isEmpty()) {
                    String or = trip.getOrigin();
                    if (or == null) continue;
                    if (!normalize(or).contains(normalize(selectedPickup))) continue;
                }
                if (selectedDropoff != null && !selectedDropoff.isEmpty()) {
                    String de = trip.getDestination();
                    if (de == null) continue;
                    if (!normalize(de).contains(normalize(selectedDropoff))) continue;
                }

                // Search query filter
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    String haystack = "";
                    if (trip.getOperator() != null) haystack += trip.getOperator() + " ";
                    if (trip.getOrigin() != null) haystack += trip.getOrigin() + " ";
                    if (trip.getDestination() != null) haystack += trip.getDestination() + " ";
                    if (trip.getBusType() != null) haystack += trip.getBusType() + " ";
                    if (!haystack.toLowerCase(Locale.getDefault()).contains(searchQuery)) continue;
                }

                filteredTrips.add(trip);
            }

            // Sorting
            if (sortMode != 0) {
                switch (sortMode) {
                    case 1:
                        Collections.sort(filteredTrips, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                        break;
                    case 2:
                        Collections.sort(filteredTrips, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                        break;
                    case 3:
                        Collections.sort(filteredTrips, (a, b) -> {
                            double aHour = parseToFractionalHours(a.getDepartureTime());
                            double bHour = parseToFractionalHours(b.getDepartureTime());
                            return Double.compare(aHour, bHour);
                        });
                        break;
                    case 4:
                        Collections.sort(filteredTrips, (a, b) -> {
                            double aHour = parseToFractionalHours(a.getDepartureTime());
                            double bHour = parseToFractionalHours(b.getDepartureTime());
                            return Double.compare(bHour, aHour);
                        });
                        break;
                }
            }

            Log.d(TAG, "Filtered trips: " + filteredTrips.size() + " from " + allTrips.size());

            if (tripAdapter == null) {
                Log.e(TAG, "tripAdapter is null!");
                return;
            }

            tripAdapter.updateTrips(filteredTrips);

            if (rvTrips == null || tvEmptyState == null) {
                Log.e(TAG, "rvTrips or tvEmptyState is null!");
                return;
            }

            if (filteredTrips.isEmpty()) {
                rvTrips.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvTrips.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in applyFilters: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(TripListActivity.this, "Lỗi filter: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateSortButtonText() {
         if (btnSort == null) return;
         String txt;
         switch (sortMode) {
             case 1: txt = "Giá ↑"; break;
             case 2: txt = "Giá ↓"; break;
             case 3: txt = "Giờ ↑"; break;
             case 4: txt = "Giờ ↓"; break;
             default: txt = getString(R.string.sort);
         }
         btnSort.setText(txt);
     }

     private void showTimePresetDialog() {
        final String[] items = new String[] { "Tất cả", "Sáng (5-11)", "Trưa (12-17)", "Chiều (18-21)", "Đêm (22-4)" };
        new AlertDialog.Builder(this)
                .setTitle("Chọn khoảng giờ")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: minTime = 0; maxTime = 24; break;
                        case 1: minTime = 5; maxTime = 11; break;
                        case 2: minTime = 12; maxTime = 17; break;
                        case 3: minTime = 18; maxTime = 21; break;
                        case 4: minTime = 22; maxTime = 4; break; // overnight
                    }
                    applyFilters();
                })
                .show();
    }

     private void showOperatorDialog() {
         // If we have no cached operators and no trips loaded yet, fetch from API first and show loading
         if ((operatorsCache == null || operatorsCache.isEmpty()) && (allTrips == null || allTrips.isEmpty())) {
             if (isFinishing() || isDestroyed()) return; // activity not suitable to show dialogs
             if (isFetchingOperators) return; // already fetching; avoid duplicate
             isFetchingOperators = true;
             final AlertDialog loading = new AlertDialog.Builder(this)
                     .setTitle("Nhà xe")
                     .setMessage("Đang tải danh sách nhà xe...")
                     .setCancelable(false)
                     .create();
             activeDialog = loading;
             loading.show();
             fetchOperators(() -> {
                 // run on UI thread to safely interact with dialogs/activity
                 runOnUiThread(() -> {
                     try {
                         if (activeDialog != null && activeDialog.isShowing()) {
                             activeDialog.dismiss();
                         }
                     } catch (Exception ignored) {}
                     activeDialog = null;
                     isFetchingOperators = false;
                     if (isFinishing() || isDestroyed()) return;
                     showOperatorDialog();
                 });
             });
             return;
         }
         // Use cached operators if available, else fall back to collecting from trips
         final List<String> ops = new ArrayList<>();
         ops.add("Tất cả");
         if (operatorsCache != null && !operatorsCache.isEmpty()) {
             for (String o : operatorsCache) if (o != null && !ops.contains(o)) ops.add(o);
         } else {
             for (Trip t : allTrips) if (t.getOperator() != null && !ops.contains(t.getOperator())) ops.add(t.getOperator());
         }
         final String[] items = ops.toArray(new String[0]);
         int initial = 0;
         if (selectedOperator != null && !selectedOperator.isEmpty()) {
             initial = Math.max(0, ops.indexOf(selectedOperator));
         }
         if (isFinishing() || isDestroyed()) return;
         AlertDialog dialog = new AlertDialog.Builder(this)
                 .setTitle("Nhà xe")
                 .setSingleChoiceItems(items, initial, (d, which) -> {
                     String pick = items[which];
                     if ("Tất cả".equals(pick)) {
                         selectedOperator = "";
                         if (btnOperator != null) btnOperator.setText(getString(R.string.operator));
                     } else {
                         selectedOperator = pick;
                         if (btnOperator != null) btnOperator.setText(pick);
                     }
                     try { d.dismiss(); } catch (Exception ignored) {}
                     applyFilters();
                 })
                 .create();
         activeDialog = dialog;
         dialog.show();
     }

    private void fetchOperators() { fetchOperators(null); }

    private void fetchOperators(Runnable onDone) {
        if (apiService == null) {
            if (onDone != null) onDone.run();
            return;
        }
        // Prevent concurrent fetches
        if (isFetchingOperators) {
            if (onDone != null) onDone.run();
            return;
        }
        isFetchingOperators = true;
        apiService.getOperators().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                isFetchingOperators = false;
                if (response.isSuccessful() && response.body() != null) {
                    operatorsCache = new ArrayList<>();
                    for (String o : response.body()) if (o != null) operatorsCache.add(o);
                } else {
                    Log.i(TAG, "getOperators returned non-success: " + (response == null ? "null" : response.code()));
                }
                if (onDone != null) onDone.run();
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                isFetchingOperators = false;
                Log.w(TAG, "Failed to fetch operators: " + (t == null ? "unknown" : t.getMessage()));
                if (onDone != null) onDone.run();
            }
        });
    }

    // Helper: parse a time string into fractional hours (e.g., 14:30 -> 14.5)
    // Accepts formats like "HH:mm", "HH:mm:ss", and timestamps that contain a HH:mm segment
    private double parseToFractionalHours(String timeStr) {
        if (timeStr == null) return Double.NaN;
        try {
            Pattern p = Pattern.compile("([01]?\\d|2[0-3]):([0-5]\\d)(?::([0-5]\\d))?");
            Matcher m = p.matcher(timeStr);
            if (m.find()) {
                String g1 = m.group(1);
                String g2 = m.group(2);
                if (g1 == null || g2 == null) return Double.NaN;
                int hour = Integer.parseInt(g1);
                int minute = Integer.parseInt(g2);
                return hour + minute / 60.0;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing time: " + timeStr, e);
        }
        return Double.NaN;
    }

    // Helper: checks if a departure hour is inside [start, end] range, supporting overnight ranges
    private boolean isInTimeRange(double dep, double start, double end) {
        if (start <= end) {
            return dep >= start && dep <= end;
        } else {
            // Overnight range (e.g., 22 to 4) -> true if dep >= start OR dep <= end
            return dep >= start || dep <= end;
        }
    }
}

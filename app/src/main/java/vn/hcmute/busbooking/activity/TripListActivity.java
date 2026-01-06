package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
import java.util.Calendar;
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
    private String returnDate = null; // optional return date in dd/MM/yyyy

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
        // Prefer new key 'isRoundTrip' but accept legacy 'isReturn' for backward compatibility
        isReturn = getIntent().getBooleanExtra("isRoundTrip", getIntent().getBooleanExtra("isReturn", false));
        // read optional return date passed from previous activity (MainActivity / GuestHome)
        String r = getIntent().getStringExtra("returnDate");
        if (r != null && !r.isEmpty()) returnDate = r;

        if (origin == null) origin = "";
        if (destination == null) destination = "";
        if (travelDate == null) travelDate = "Hôm nay";

        tvRoute.setText(String.format(Locale.getDefault(), "%s → %s", origin, destination));
        tvDate.setText(String.format(Locale.getDefault(), "Ngày: %s", travelDate));

        if (tvChange != null) {
            tvChange.setOnClickListener(v -> showSearchUpperSheet());
        }

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(this, new ArrayList<Trip>(), new ArrayList<Integer>());
        rvTrips.setAdapter(tripAdapter);

        tripAdapter.setOnItemClickListener(trip -> {
            Intent intent = new Intent(TripListActivity.this, TripDetailActivity.class);
            intent.putExtra("trip_id", trip.getId()); // Fix: pass trip_id so detail activity can load data
            intent.putExtra("trip", trip);
            // Forward round-trip indicator (use 'isRoundTrip')
            if (isReturn) intent.putExtra("isRoundTrip", true);
            intent.putExtra("returnOrigin", destination);
            intent.putExtra("returnDestination", origin);
            // Pass the actual return date (if set) instead of departure date
            if (isReturn && returnDate != null && !returnDate.isEmpty()) {
                intent.putExtra("returnDate", returnDate);
            }

            // If this TripList was launched as part of a round-trip "return selection" phase,
            // forward any depart_* extras so downstream activities can assemble both legs.
            try {
                Intent src = getIntent();
                boolean roundPhase = src.getBooleanExtra("round_trip_phase", false);
                if (roundPhase) {
                    // forward depart trip/seat/pickup/dropoff if present
                    android.os.Parcelable departTrip = src.getParcelableExtra("depart_trip");
                    java.util.ArrayList<String> departSeats = src.getStringArrayListExtra("depart_seat_labels");
                    android.os.Parcelable departPickup = src.getParcelableExtra("depart_pickup_location");
                    android.os.Parcelable departDropoff = src.getParcelableExtra("depart_dropoff_location");
                    if (departTrip != null) intent.putExtra("depart_trip", departTrip);
                    if (departSeats != null) intent.putStringArrayListExtra("depart_seat_labels", departSeats);
                    if (departPickup != null) intent.putExtra("depart_pickup_location", departPickup);
                    if (departDropoff != null) intent.putExtra("depart_dropoff_location", departDropoff);
                    // keep the round trip phase flag in the chain
                    intent.putExtra("round_trip_phase", true);
                    // Also preserve high-level isRoundTrip indicator
                    intent.putExtra("isRoundTrip", true);
                }
            } catch (Exception ignored) {}

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
                if (response.isSuccessful() && response.body() != null) {
                    List<Trip> trips = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        trips.add(new Trip(map));
                    }
                    allTrips = trips;
                    applyFilters();
                } else {
                    allTrips.clear();
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                allTrips.clear();
                applyFilters();
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
        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : allTrips) {
            // Price filter
            if (!(trip.getPrice() >= minPrice && trip.getPrice() <= maxPrice)) continue;

            // Time filter
            String departure = trip.getDepartureTime();
            double depHour = parseToFractionalHours(departure);
            if (Double.isNaN(depHour)) {
                Log.w(TAG, "Unparsable departure time: " + departure);
                continue; // exclude unparsable times
            }

            if (!isInTimeRange(depHour, this.minTime, this.maxTime)) continue;

            // Operator filter (if any selected)
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

            // Pickup/dropoff filter (we map pickup->origin, dropoff->destination)
            if (selectedPickup != null && !selectedPickup.isEmpty()) {
                String or = trip.getOrigin(); if (or == null) continue;
                if (!normalize(or).contains(normalize(selectedPickup))) continue;
            }
            if (selectedDropoff != null && !selectedDropoff.isEmpty()) {
                String de = trip.getDestination(); if (de == null) continue;
                if (!normalize(de).contains(normalize(selectedDropoff))) continue;
            }

            // Search query filter: match on operator, origin, destination, bus name (if provided)
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
                case 1: // price asc
                    Collections.sort(filteredTrips, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                    break;
                case 2: // price desc
                    Collections.sort(filteredTrips, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                    break;
                case 3: // time asc
                    Collections.sort(filteredTrips, (a, b) -> Double.compare(parseToFractionalHours(a.getDepartureTime()), parseToFractionalHours(b.getDepartureTime())));
                    break;
                case 4: // time desc
                    Collections.sort(filteredTrips, (a, b) -> Double.compare(parseToFractionalHours(b.getDepartureTime()), parseToFractionalHours(a.getDepartureTime())));
                    break;
            }
        }

        tripAdapter.updateTrips(filteredTrips);

        if (filteredTrips.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTrips.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTrips.setVisibility(View.VISIBLE);
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

    /**
     * Show upper sheet dialog for changing search criteria (origin, destination, date)
     */
    private void showSearchUpperSheet() {
        Dialog dialog = new Dialog(this, R.style.UpperSheetDialogStyle);
        View sheetView = getLayoutInflater().inflate(R.layout.upper_sheet_search, null);
        dialog.setContentView(sheetView);

        // Set dialog window properties to display at top
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        // Find views
        AutoCompleteTextView etOrigin = sheetView.findViewById(R.id.etOrigin);
        AutoCompleteTextView etDestination = sheetView.findViewById(R.id.etDestination);
        TextView tvDateDialog = sheetView.findViewById(R.id.tvDate);
        androidx.appcompat.widget.SwitchCompat switchReturn = sheetView.findViewById(R.id.switchReturn);
        MaterialButton btnSearch = sheetView.findViewById(R.id.btnSearchTrips);
        MaterialButton btnClose = sheetView.findViewById(R.id.btnCloseSearch);
        ImageView ivSwap = sheetView.findViewById(R.id.ivSwap);

        // Handle close button click
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Handle swap button click
        if (ivSwap != null) {
            ivSwap.setOnClickListener(v -> {
                String from = etOrigin.getText().toString();
                String to = etDestination.getText().toString();
                etOrigin.setText(to);
                etDestination.setText(from);
            });
        }

        // Setup location dropdown
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột", "Vũng Tàu", "Cam Ranh"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);

        // Set current values
        etOrigin.setText(origin, false);
        etDestination.setText(destination, false);
        // Show departure or dep → ret if returnDate present
        if (isReturn && returnDate != null && !returnDate.isEmpty()) {
            tvDateDialog.setText(String.format(Locale.getDefault(), "%s → %s", travelDate, returnDate));
            switchReturn.setChecked(true);
        } else {
            tvDateDialog.setText(travelDate);
            switchReturn.setChecked(false);
        }

        // Setup date handling
        final Calendar departCal = Calendar.getInstance();
        final Calendar retCal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = sdf.parse(travelDate);
            if (d != null) departCal.setTime(d);
            if (returnDate != null && !returnDate.isEmpty()) {
                Date rDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(returnDate);
                if (rDate != null) retCal.setTime(rDate);
            }
        } catch (Exception ignored) {}

        // When tvDateDialog clicked: pick departure date
        tvDateDialog.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                departCal.set(year, month, dayOfMonth);
                // If return exists and is before depart, clear return
                if (switchReturn.isChecked() && retCal.before(departCal)) {
                    // invalid, clear return
                    returnDate = null;
                    // reset retCal to depart
                    retCal.setTime(departCal.getTime());
                    switchReturn.setChecked(false);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                if (switchReturn.isChecked() && returnDate != null) tvDateDialog.setText(String.format(Locale.getDefault(), "%s → %s", sdf.format(departCal.getTime()), returnDate));
                else tvDateDialog.setText(sdf.format(departCal.getTime()));
            }, departCal.get(Calendar.YEAR), departCal.get(Calendar.MONTH), departCal.get(Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dp.show();
        });

        // When switchReturn toggled, if turned on open return date picker immediately; if turned off clear return date
        switchReturn.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                // open return date picker
                DatePickerDialog rdr = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    retCal.set(year, month, dayOfMonth);
                    // Ensure return not before depart
                    if (retCal.before(departCal)) {
                        Toast.makeText(TripListActivity.this, "Ngày về không thể trước ngày đi", Toast.LENGTH_LONG).show();
                        returnDate = null;
                        switchReturn.setChecked(false);
                        tvDateDialog.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(departCal.getTime()));
                        return;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    returnDate = sdf.format(retCal.getTime());
                    tvDateDialog.setText(String.format(Locale.getDefault(), "%s → %s", sdf.format(departCal.getTime()), returnDate));
                }, retCal.get(Calendar.YEAR), retCal.get(Calendar.MONTH), retCal.get(Calendar.DAY_OF_MONTH));
                rdr.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                rdr.show();
            } else {
                // clear return
                returnDate = null;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvDateDialog.setText(sdf.format(departCal.getTime()));
            }
        });

        // Handle search button click
        btnSearch.setOnClickListener(v -> {
            String newOrigin = etOrigin.getText().toString().trim();
            String newDestination = etDestination.getText().toString().trim();
            if (newOrigin.isEmpty() || newDestination.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn điểm đi và điểm đến", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update current values
            origin = newOrigin;
            destination = newDestination;

            // Update depart date from departCal
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            travelDate = sdf.format(departCal.getTime());

            // isReturn flag and returnDate already managed via switchReturn and returnDate variable
            isReturn = switchReturn.isChecked();

            // Update UI - show depart or dep → ret
            if (isReturn && returnDate != null && !returnDate.isEmpty()) {
                this.tvDate.setText(String.format(Locale.getDefault(), "Ngày: %s → %s", travelDate, returnDate));
            } else {
                this.tvDate.setText(String.format(Locale.getDefault(), "Ngày: %s", travelDate));
            }

            // Fetch new trips
            fetchTrips(origin, destination, travelDate);

            // Dismiss dialog
            dialog.dismiss();
        });

        dialog.show();
    }
}

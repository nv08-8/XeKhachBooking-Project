package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.RevenueAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class RevenueStatsActivity extends AppCompatActivity implements RevenueAdapter.OnItemClickListener {
    private ProgressBar progressRevenue;
    private RecyclerView rvRevenue;
    private TextView tvEmptyRevenue;
    private Spinner spinnerGroupBy, spinnerRoutes, spinnerTrips;
    private Button btnApplyFilter;
    private EditText etStartDate, etEndDate;
    private LinearLayout dateRangeFilter;
    private BarChart barChart;
    private NestedScrollView scrollView;
    private Toolbar toolbar;
    private com.google.android.material.tabs.TabLayout tabRevenueType;

    private RevenueAdapter adapter;
    private List<Map<String, Object>> revenueList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();

    private String[] groupByValues = {"day", "month", "year", "route", "trip"};

    private List<Map<String, Object>> routeList = new ArrayList<>();
    private List<Trip> tripList = new ArrayList<>();

    // Biến để track loại báo cáo hiện tại
    private boolean isRefundMode = false; // false = doanh thu, true = hoàn tiền

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());

        // Find Views
        progressRevenue = findViewById(R.id.progressRevenue);
        rvRevenue = findViewById(R.id.rvRevenue);
        tvEmptyRevenue = findViewById(R.id.tvEmptyRevenue);
        spinnerGroupBy = findViewById(R.id.spinnerGroupBy);
        spinnerRoutes = findViewById(R.id.spinnerRoutes);
        spinnerTrips = findViewById(R.id.spinnerTrips);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        tabRevenueType = findViewById(R.id.tabRevenueType);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        dateRangeFilter = findViewById(R.id.dateRangeFilter);
        barChart = findViewById(R.id.barChart);
        scrollView = findViewById(R.id.scrollView);

        // Setup
        rvRevenue.setLayoutManager(new LinearLayoutManager(this));
        setDefaultDates();
        setupFilterListeners();

        // Initial data fetch
        fetchRoutes();
    }

    private void setDefaultDates() {
        startCalendar.set(Calendar.DAY_OF_MONTH, 1); // First day of the current month
        endCalendar = Calendar.getInstance(); // Today
        updateEditText(etStartDate, startCalendar);
        updateEditText(etEndDate, endCalendar);
    }

    private void setupFilterListeners() {
        btnApplyFilter.setOnClickListener(v -> fetchRevenueStats());

        etStartDate.setOnClickListener(v -> showDatePickerDialog(etStartDate, startCalendar));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(etEndDate, endCalendar));

        // TabLayout listener để chuyển đổi giữa Doanh thu và Hoàn tiền
        tabRevenueType.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                isRefundMode = (tab.getPosition() == 1); // Tab 0 = doanh thu, Tab 1 = hoàn tiền
                // Tải lại dữ liệu khi chuyển tab
                if (spinnerGroupBy.getSelectedItemPosition() >= 0) {
                    fetchRevenueStats();
                }
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        spinnerGroupBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGroupBy = groupByValues[position];
                dateRangeFilter.setVisibility(selectedGroupBy.equals("day") ? View.VISIBLE : View.GONE);
                spinnerRoutes.setVisibility(selectedGroupBy.equals("trip") || selectedGroupBy.equals("route") ? View.VISIBLE : View.GONE);
                spinnerTrips.setVisibility(selectedGroupBy.equals("trip") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerRoutes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (routeList.isEmpty() || position < 0 || position >= routeList.size()) return;
                Map<String, Object> selectedRoute = routeList.get(position);
                int routeId = getIntFromMap(selectedRoute, "id");
                fetchTripsForRoute(routeId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int getIntFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return (int) Double.parseDouble((String) val); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private float getFloatFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).floatValue();
        if (val instanceof String) {
            try { return Float.parseFloat((String) val); } catch (Exception e) { return 0f; }
        }
        return 0f;
    }

    private void fetchRoutes() {
        apiService.getRoutes(null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    routeList.clear();
                    routeList.addAll(response.body());
                    List<String> routeNames = new ArrayList<>();
                    for (Map<String, Object> route : routeList) {
                        routeNames.add(route.get("origin") + " - " + route.get("destination"));
                    }
                    ArrayAdapter<String> routeAdapter = new ArrayAdapter<>(RevenueStatsActivity.this, android.R.layout.simple_spinner_item, routeNames);
                    routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRoutes.setAdapter(routeAdapter);

                    // Fetch initial revenue stats after routes are loaded
                    fetchRevenueStats();
                } else {
                    Toast.makeText(RevenueStatsActivity.this, "Lỗi khi tải danh sách tuyến", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(RevenueStatsActivity.this, "Lỗi khi tải danh sách tuyến: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTripsForRoute(int routeId) {
        apiService.getTrips(routeId, null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tripList.clear();
                    for (Map<String, Object> tripMap : response.body()) {
                        tripList.add(new Trip(tripMap));
                    }

                    List<String> tripNames = new ArrayList<>();
                    for (Trip trip : tripList) {
                        tripNames.add("Chuyến " + trip.getId() + " - " + trip.getDepartureTime());
                    }
                    ArrayAdapter<String> tripAdapter = new ArrayAdapter<>(RevenueStatsActivity.this, android.R.layout.simple_spinner_item, tripNames);
                    tripAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTrips.setAdapter(tripAdapter);
                } else {
                    Toast.makeText(RevenueStatsActivity.this, "Lỗi khi tải danh sách chuyến", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(RevenueStatsActivity.this, "Lỗi khi tải danh sách chuyến: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerDialog(EditText editText, Calendar calendar) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateEditText(editText, calendar);
        };

        new DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateEditText(EditText editText, Calendar calendar) {
        String myFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }

    private void fetchRevenueStats() {
        progressRevenue.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);
        tvEmptyRevenue.setVisibility(View.GONE);

        int groupByPos = spinnerGroupBy.getSelectedItemPosition();
        if (groupByPos < 0) return; // Don't fetch if spinner is not ready

        String groupBy = groupByValues[groupByPos];
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();
        Integer routeId = null;
        Integer tripId = null;

        int routePos = spinnerRoutes.getSelectedItemPosition();
        if (spinnerRoutes.getVisibility() == View.VISIBLE && routePos >= 0 && routePos < routeList.size()) {
            routeId = getIntFromMap(routeList.get(routePos), "id");
        }

        int tripPos = spinnerTrips.getSelectedItemPosition();
        if (spinnerTrips.getVisibility() == View.VISIBLE && tripPos >= 0 && tripPos < tripList.size()) {
            tripId = tripList.get(tripPos).getId();
        }

        // Gọi API khác nhau tùy theo mode
        Call<List<Map<String, Object>>> call;
        if (isRefundMode) {
            call = apiService.getRevenueRefunds(sessionManager.getUserId(), groupBy, routeId, tripId, startDate, endDate);
        } else {
            call = apiService.getRevenue(sessionManager.getUserId(), groupBy, routeId, tripId, startDate, endDate);
        }

        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressRevenue.setVisibility(View.GONE);
                if (!response.isSuccessful()) {
                    tvEmptyRevenue.setText("Không thể tải báo cáo (code=" + response.code() + ")");
                    tvEmptyRevenue.setVisibility(View.VISIBLE);
                    return;
                }

                List<Map<String, Object>> revenues = response.body();

                if (revenues == null || revenues.isEmpty()) {
                    tvEmptyRevenue.setText("Không có dữ liệu báo cáo");
                    tvEmptyRevenue.setVisibility(View.VISIBLE);
                    barChart.setVisibility(View.GONE);
                    rvRevenue.setVisibility(View.GONE);
                } else {
                    revenueList.clear();
                    revenueList.addAll(revenues);
                    adapter = new RevenueAdapter(revenueList, RevenueStatsActivity.this);
                    rvRevenue.setAdapter(adapter);
                    scrollView.setVisibility(View.VISIBLE);
                    rvRevenue.setVisibility(View.VISIBLE);
                    barChart.setVisibility(View.VISIBLE);
                    setupBarChart();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressRevenue.setVisibility(View.GONE);
                tvEmptyRevenue.setText("Lỗi: " + t.getMessage());
                tvEmptyRevenue.setVisibility(View.VISIBLE);
            }
        });
    }

    private String formatDateValue(Object dateObj) {
        if (dateObj == null) return "";
        String dateStr = dateObj.toString();
        try {
            if (dateStr.contains("T")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = sdf.parse(dateStr);
                SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return newSdf.format(date);
            }
        } catch (ParseException e) {
        }
        return dateStr;
    }

    private void setupBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < revenueList.size(); i++) {
            Map<String, Object> revenue = revenueList.get(i);
            float totalRevenue = getFloatFromMap(revenue, "total_revenue");
            entries.add(new BarEntry(i, totalRevenue));

            String groupBy = groupByValues[spinnerGroupBy.getSelectedItemPosition()];
            switch (groupBy) {
                case "day":
                    labels.add(formatDateValue(revenue.get("group_key")));
                    break;
                case "month":
                case "year":
                    labels.add(String.valueOf(revenue.get("group_key")));
                    break;
                case "route":
                    labels.add(revenue.get("origin") + " - " + revenue.get("destination"));
                    break;
                case "trip":
                     labels.add("Chuyến " + revenue.get("group_key"));
                    break;
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                handleItemClick(revenueList.get((int) e.getX()));
            }

            @Override
            public void onNothingSelected() {}
        });
    }

    @Override
    public void onItemClick(Map<String, Object> revenue) {
        handleItemClick(revenue);
    }

    private void handleItemClick(Map<String, Object> revenue) {
        String groupBy = groupByValues[spinnerGroupBy.getSelectedItemPosition()];
        String value = "";
        // For details, we usually need an ID or a specific date/month/year string
        switch (groupBy) {
            case "day":
                value = formatDateValue(revenue.get("group_key"));
                break;
            case "month":
            case "year":
                 value = String.valueOf(revenue.get("group_key"));
                break;
            case "route":
            case "trip":
                // In the consolidated API, the ID is the group_key for route and trip
                Object idObj = revenue.get("group_key");
                if (idObj != null) {
                    value = String.valueOf(idObj);
                }
                break;
        }

        if (!value.isEmpty()) {
            openDetailsActivity(groupBy, value);
        }
    }

    private void openDetailsActivity(String groupBy, String value) {
        Intent intent = new Intent(this, RevenueDetailsActivity.class);
        intent.putExtra("groupBy", groupBy);
        intent.putExtra("value", value);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

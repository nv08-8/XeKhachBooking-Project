package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

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
import vn.hcmute.busbooking.utils.SessionManager;

public class RevenueStatsActivity extends AppCompatActivity implements RevenueAdapter.OnItemClickListener {
    private ProgressBar progressRevenue;
    private RecyclerView rvRevenue;
    private TextView tvEmptyRevenue;
    private Spinner spinnerGroupBy;
    private Button btnApplyFilter;
    private EditText etStartDate, etEndDate;
    private LinearLayout dateRangeFilter;
    private BarChart barChart;
    private NestedScrollView scrollView;
    private Toolbar toolbar;

    private RevenueAdapter adapter;
    private List<Map<String, Object>> revenueList = new ArrayList<>();
    private SessionManager sessionManager;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();

    private String[] groupByValues = {"day", "month", "year", "route"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        sessionManager = new SessionManager(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressRevenue = findViewById(R.id.progressRevenue);
        rvRevenue = findViewById(R.id.rvRevenue);
        tvEmptyRevenue = findViewById(R.id.tvEmptyRevenue);
        spinnerGroupBy = findViewById(R.id.spinnerGroupBy);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        dateRangeFilter = findViewById(R.id.dateRangeFilter);
        barChart = findViewById(R.id.barChart);
        scrollView = findViewById(R.id.scrollView);

        // Setup RecyclerView
        rvRevenue.setLayoutManager(new LinearLayoutManager(this));

        setupFilterListeners();
        fetchRevenueStats();
    }

    private void setupFilterListeners() {
        btnApplyFilter.setOnClickListener(v -> fetchRevenueStats());

        etStartDate.setOnClickListener(v -> showDatePickerDialog(etStartDate, startCalendar));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(etEndDate, endCalendar));

        spinnerGroupBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Day
                    dateRangeFilter.setVisibility(View.VISIBLE);
                } else {
                    dateRangeFilter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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

        String groupBy = groupByValues[spinnerGroupBy.getSelectedItemPosition()];

        ApiService api = ApiClient.getClient().create(ApiService.class);
        int userId = sessionManager.getUserId();

        Call<List<Map<String, Object>>> call;

        if (groupBy.equals("day")) {
            String startDate = etStartDate.getText().toString();
            String endDate = etEndDate.getText().toString();
            call = api.getRevenueByDate(userId, startDate, endDate);
        } else if (groupBy.equals("month")) {
            call = api.getRevenueByMonth(userId);
        } else if (groupBy.equals("year")) {
            call = api.getRevenueByYear(userId);
        } else { // route
            call = api.getRevenueByRoute(userId);
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
                } else {
                    revenueList.clear();
                    revenueList.addAll(revenues);
                    adapter = new RevenueAdapter(revenueList, RevenueStatsActivity.this);
                    rvRevenue.setAdapter(adapter);
                    scrollView.setVisibility(View.VISIBLE);
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
            // Handle full date-time format
            if (dateStr.contains("T")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = sdf.parse(dateStr);
                SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return newSdf.format(date);
            }
        } catch (ParseException e) {
            // Fallback for other formats or if parsing fails
        }
        return dateStr;
    }


    private void setupBarChart() {
        barChart.setVisibility(View.VISIBLE);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < revenueList.size(); i++) {
            Map<String, Object> revenue = revenueList.get(i);
            float totalRevenue = Float.parseFloat(revenue.get("total_revenue").toString());
            entries.add(new BarEntry(i, totalRevenue));

            if (revenue.containsKey("date")) {
                labels.add(formatDateValue(revenue.get("date")));
            } else if (revenue.containsKey("month")) {
                labels.add(revenue.get("month").toString());
            } else if (revenue.containsKey("year")) {
                labels.add(revenue.get("year").toString());
            } else if (revenue.containsKey("route")) {
                labels.add(revenue.get("route").toString());
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
        barChart.invalidate(); // refresh

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
        if (revenue.containsKey("date")) {
            value = formatDateValue(revenue.get("date"));
        } else if (revenue.containsKey("month")) {
            value = revenue.get("month").toString();
        } else if (revenue.containsKey("year")) {
            value = revenue.get("year").toString();
        } else if (revenue.containsKey("route")) {
            value = revenue.get("route").toString();
        }
        openDetailsActivity(groupBy, value);
    }

    private void openDetailsActivity(String groupBy, String value) {
        Intent intent = new Intent(this, RevenueDetailsActivity.class);
        intent.putExtra("groupBy", groupBy);
        intent.putExtra("value", value);
        startActivity(intent);
    }
}

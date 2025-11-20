package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class RevenueStatsActivity extends AppCompatActivity {

    private TextView tvTotalRevenueValue, tvTotalTicketsValue, tvTopRouteValue;
    private TextInputEditText etFromDate, etToDate;
    private ImageButton btnBack;
    private Spinner spinnerGroupBy;
    private BarChart barChart;
    private ApiService apiService;
    private Calendar fromCalendar, toCalendar;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        tvTotalRevenueValue = findViewById(R.id.tvTotalRevenueValue);
        tvTotalTicketsValue = findViewById(R.id.tvTotalTicketsValue);
        tvTopRouteValue = findViewById(R.id.tvTopRouteValue);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        btnBack = findViewById(R.id.btnBack);
        spinnerGroupBy = findViewById(R.id.spinnerGroupBy);
        barChart = findViewById(R.id.barChart);

        apiService = ApiClient.getClient().create(ApiService.class);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialize dates
        fromCalendar = Calendar.getInstance();
        fromCalendar.add(Calendar.DAY_OF_YEAR, -7); // Default last 7 days
        toCalendar = Calendar.getInstance();

        updateDateFields();

        etFromDate.setOnClickListener(v -> showDatePicker(fromCalendar, etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(toCalendar, etToDate));

        btnBack.setOnClickListener(v -> finish());

        spinnerGroupBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshStats();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initial load
        refreshStats();
    }

    private void showDatePicker(Calendar calendar, TextInputEditText editText) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            editText.setText(dateFormat.format(calendar.getTime()));
            refreshStats();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateFields() {
        etFromDate.setText(dateFormat.format(fromCalendar.getTime()));
        etToDate.setText(dateFormat.format(toCalendar.getTime()));
    }

    private void refreshStats() {
        String groupBy;
        int selectedPosition = spinnerGroupBy.getSelectedItemPosition();
        if (selectedPosition == 1) {
            groupBy = "month";
        } else if (selectedPosition == 2) {
            groupBy = "year";
        } else {
            groupBy = "day";
        }
        
        String from = dateFormat.format(fromCalendar.getTime());
        String to = dateFormat.format(toCalendar.getTime());
        
        loadStats(groupBy, from, to);
    }

    private void loadStats(String groupBy, String from, String to) {
        Call<Map<String, Object>> call = apiService.getRevenueStats(groupBy, from, to);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    
                    Object totalRev = data.get("total_revenue");
                    Object totalTickets = data.get("total_tickets");
                    Object topRoute = data.get("top_route");
                    
                    if (totalRev != null) tvTotalRevenueValue.setText(String.format("%,.0f VNĐ", Double.parseDouble(String.valueOf(totalRev))));
                    else tvTotalRevenueValue.setText("0 VNĐ");
                    
                    if (totalTickets != null) tvTotalTicketsValue.setText(String.valueOf(totalTickets));
                    else tvTotalTicketsValue.setText("0");
                    
                    if (topRoute != null) tvTopRouteValue.setText(String.valueOf(topRoute));
                    else tvTopRouteValue.setText("-");

                    // Chart
                    Object seriesObj = data.get("series");
                    if (seriesObj instanceof List) {
                        List<?> series = (List<?>) seriesObj;
                        List<BarEntry> entries = new ArrayList<>();
                        List<String> labels = new ArrayList<>();
                        
                        int i = 0;
                        for (Object item : series) {
                            if (item instanceof Map) {
                                Map<?, ?> m = (Map<?, ?>) item;
                                Object val = m.get("value");
                                Object label = m.get("label"); // Assuming API returns label for x-axis
                                
                                float v = 0f;
                                try { v = Float.parseFloat(String.valueOf(val)); } catch (Exception ignored) {}
                                entries.add(new BarEntry(i++, v));
                                labels.add(label != null ? String.valueOf(label) : "");
                            }
                        }
                        
                        BarDataSet set = new BarDataSet(entries, "Doanh thu");
                        BarData barData = new BarData(set);
                        barChart.setData(barData);
                        barChart.getDescription().setEnabled(false);
                        
                        XAxis x = barChart.getXAxis();
                        x.setPosition(XAxis.XAxisPosition.BOTTOM);
                        x.setValueFormatter(new IndexAxisValueFormatter(labels));
                        x.setGranularity(1f);
                        x.setLabelRotationAngle(-45);
                        
                        barChart.animateY(1000);
                        barChart.invalidate();
                    }

                } else {
                    Toast.makeText(RevenueStatsActivity.this, "Không thể lấy dữ liệu thống kê", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RevenueStatsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

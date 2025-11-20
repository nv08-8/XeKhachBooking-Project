package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.RevenueAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class RevenueStatsActivity extends AppCompatActivity {
    private ProgressBar progressRevenue;
    private RecyclerView rvRevenue;
    private TextView tvEmptyRevenue;
    private Spinner spinnerGroupBy;
    private Button btnApplyFilter;
    private RevenueAdapter adapter;
    private List<Map<String, Object>> revenueList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        progressRevenue = findViewById(R.id.progressRevenue);
        rvRevenue = findViewById(R.id.rvRevenue);
        tvEmptyRevenue = findViewById(R.id.tvEmptyRevenue);
        spinnerGroupBy = findViewById(R.id.spinnerGroupBy);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);

        // Setup RecyclerView
        rvRevenue.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RevenueAdapter(revenueList);
        rvRevenue.setAdapter(adapter);

        btnApplyFilter.setOnClickListener(v -> fetchRevenueStats());

        fetchRevenueStats();
    }

    private void fetchRevenueStats() {
        progressRevenue.setVisibility(View.VISIBLE);
        rvRevenue.setVisibility(View.GONE);
        tvEmptyRevenue.setVisibility(View.GONE);

        String groupBy = "day"; // mặc định
        int selected = spinnerGroupBy.getSelectedItemPosition();
        if (selected == 1) groupBy = "month";
        else if (selected == 2) groupBy = "year";
        else if (selected == 3) groupBy = "route";

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Map<String, Object>> call = api.getRevenueStats(groupBy, null, null);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressRevenue.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEmptyRevenue.setText("Không thể tải báo cáo (code=" + response.code() + ")");
                    tvEmptyRevenue.setVisibility(View.VISIBLE);
                    return;
                }

                Map<String, Object> result = response.body();
                List<Map<String, Object>> revenues = null;

                // Try multiple possible response keys
                if (result.containsKey("data")) {
                    revenues = (List<Map<String, Object>>) result.get("data");
                } else if (result.containsKey("stats")) {
                    revenues = (List<Map<String, Object>>) result.get("stats");
                } else if (result.containsKey("revenue")) {
                    revenues = (List<Map<String, Object>>) result.get("revenue");
                } else {
                    // Log toàn bộ response để debug
                    android.util.Log.d("RevenueStats", "Response: " + result.toString());
                }

                if (revenues == null || revenues.isEmpty()) {
                    tvEmptyRevenue.setText("Không có dữ liệu báo cáo");
                    tvEmptyRevenue.setVisibility(View.VISIBLE);
                } else {
                    revenueList.clear();
                    revenueList.addAll(revenues);
                    adapter.notifyDataSetChanged();
                    rvRevenue.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressRevenue.setVisibility(View.GONE);
                tvEmptyRevenue.setText("Lỗi: " + t.getMessage());
                tvEmptyRevenue.setVisibility(View.VISIBLE);
            }
        });
    }
}

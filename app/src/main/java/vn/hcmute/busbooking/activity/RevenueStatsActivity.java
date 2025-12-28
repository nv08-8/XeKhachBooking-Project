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
import vn.hcmute.busbooking.utils.SessionManager;

public class RevenueStatsActivity extends AppCompatActivity {
    private ProgressBar progressRevenue;
    private RecyclerView rvRevenue;
    private TextView tvEmptyRevenue;
    private Spinner spinnerGroupBy;
    private Button btnApplyFilter;
    private RevenueAdapter adapter;
    private List<Map<String, Object>> revenueList = new ArrayList<>();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        sessionManager = new SessionManager(this);

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
        int userId = sessionManager.getUserId();

        Call<List<Map<String, Object>>> call;

        switch (groupBy) {
            case "month":
                call = api.getRevenueByMonth(userId);
                break;
            case "year":
                call = api.getRevenueByYear(userId);
                break;
            case "route":
                call = api.getRevenueByRoute(userId);
                break;
            case "day":
            default:
                call = api.getRevenueByDate(userId, null, null);
                break;
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
                    adapter.notifyDataSetChanged();
                    rvRevenue.setVisibility(View.VISIBLE);
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
}

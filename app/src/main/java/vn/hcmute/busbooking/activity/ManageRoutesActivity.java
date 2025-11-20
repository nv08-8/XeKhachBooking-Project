package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
import vn.hcmute.busbooking.adapter.RoutesAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class ManageRoutesActivity extends AppCompatActivity {
    private ProgressBar progressRoutes;
    private RecyclerView rvRoutes;
    private TextView tvEmptyRoutes;
    private Button btnAddRoute, btnRefreshRoutes;
    private RoutesAdapter adapter;
    private List<Map<String, Object>> routesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routes);

        progressRoutes = findViewById(R.id.progressRoutes);
        rvRoutes = findViewById(R.id.rvRoutes);
        tvEmptyRoutes = findViewById(R.id.tvEmptyRoutes);
        btnAddRoute = findViewById(R.id.btnAddRoute);
        btnRefreshRoutes = findViewById(R.id.btnRefreshRoutes);

        // Setup RecyclerView
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutesAdapter(routesList, new RoutesAdapter.OnRouteClickListener() {
            @Override
            public void onEditRoute(Map<String, Object> route) {
                Toast.makeText(ManageRoutesActivity.this, "Sửa: " + route.get("origin"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteRoute(Map<String, Object> route) {
                Toast.makeText(ManageRoutesActivity.this, "Xóa: " + route.get("origin"), Toast.LENGTH_SHORT).show();
            }
        });
        rvRoutes.setAdapter(adapter);

        btnAddRoute.setOnClickListener(v -> Toast.makeText(this, "Thêm tuyến mới", Toast.LENGTH_SHORT).show());
        btnRefreshRoutes.setOnClickListener(v -> fetchRoutes());

        fetchRoutes();
    }

    private void fetchRoutes() {
        progressRoutes.setVisibility(View.VISIBLE);
        rvRoutes.setVisibility(View.GONE);
        tvEmptyRoutes.setVisibility(View.GONE);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Map<String, Object>>> call = api.getRoutes(null, null, null);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressRoutes.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEmptyRoutes.setText("Không thể tải tuyến (code=" + response.code() + ")");
                    tvEmptyRoutes.setVisibility(View.VISIBLE);
                    return;
                }
                List<Map<String, Object>> routes = response.body();
                if (routes.isEmpty()) {
                    tvEmptyRoutes.setVisibility(View.VISIBLE);
                } else {
                    routesList.clear();
                    routesList.addAll(routes);
                    adapter.notifyDataSetChanged();
                    rvRoutes.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressRoutes.setVisibility(View.GONE);
                tvEmptyRoutes.setText("Lỗi: " + t.getMessage());
                tvEmptyRoutes.setVisibility(View.VISIBLE);
            }
        });
    }
}

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageRoutesActivity extends AppCompatActivity {
    private ProgressBar progressRoutes;
    private RecyclerView rvRoutes;
    private TextView tvEmptyRoutes;
    private Button btnAddRoute, btnRefreshRoutes;
    private RoutesAdapter adapter;
    private List<Map<String, Object>> routesList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routes);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        progressRoutes = findViewById(R.id.progressRoutes);
        rvRoutes = findViewById(R.id.rvRoutes);
        tvEmptyRoutes = findViewById(R.id.tvEmptyRoutes);
        btnAddRoute = findViewById(R.id.btnAddRoute);
        btnRefreshRoutes = findViewById(R.id.btnRefreshRoutes);

        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutesAdapter(routesList, new RoutesAdapter.OnRouteClickListener() {
            @Override
            public void onEditRoute(Map<String, Object> route) {
                Intent intent = new Intent(ManageRoutesActivity.this, RouteFormActivity.class);
                intent.putExtra("route_id", (Integer) route.get("id"));
                startActivity(intent);
            }

            @Override
            public void onDeleteRoute(Map<String, Object> route) {
                new AlertDialog.Builder(ManageRoutesActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa tuyến đường này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            int adminId = sessionManager.getUserId();
                            int routeId = ((Number) route.get("id")).intValue();
                            Call<Map<String, Object>> call = apiService.deleteRoute(adminId, routeId);
                            call.enqueue(new Callback<Map<String, Object>>() {
                                @Override
                                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(ManageRoutesActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                                        fetchRoutes();
                                    } else {
                                        Toast.makeText(ManageRoutesActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                    Toast.makeText(ManageRoutesActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvRoutes.setAdapter(adapter);

        btnAddRoute.setOnClickListener(v -> {
            Intent intent = new Intent(ManageRoutesActivity.this, RouteFormActivity.class);
            startActivity(intent);
        });
        btnRefreshRoutes.setOnClickListener(v -> fetchRoutes());

        fetchRoutes();
    }

    private void fetchRoutes() {
        progressRoutes.setVisibility(View.VISIBLE);
        rvRoutes.setVisibility(View.GONE);
        tvEmptyRoutes.setVisibility(View.GONE);

        Call<List<Map<String, Object>>> call = apiService.getRoutes(null, null, null);
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

package vn.hcmute.busbooking.activity.admin;

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
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.RouteFormActivity;
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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
            public void onRouteClick(Map<String, Object> route) {
                Intent intent = new Intent(ManageRoutesActivity.this, ManageTripsActivity.class);
                Object idObj = route.get("id");
                if (idObj != null) {
                    try {
                        int routeId = (int) Double.parseDouble(idObj.toString());
                        intent.putExtra("route_id", routeId);
                        startActivity(intent);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ManageRoutesActivity.this, "ID tuyến đường không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onEditRoute(Map<String, Object> route) {
                Intent intent = new Intent(ManageRoutesActivity.this, RouteFormActivity.class);
                Object idObj = route.get("id");
                if (idObj != null) {
                    try {
                        int routeId = (int) Double.parseDouble(idObj.toString());
                        intent.putExtra("route_id", routeId);
                        startActivity(intent);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ManageRoutesActivity.this, "ID tuyến đường không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onDeleteRoute(Map<String, Object> route) {
                new AlertDialog.Builder(ManageRoutesActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa tuyến đường này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            int adminId = sessionManager.getUserId();
                            Object idObj = route.get("id");
                            if (idObj != null) {
                                try {
                                    int routeId = (int) Double.parseDouble(idObj.toString());
                                    apiService.deleteRoute(adminId, routeId).enqueue(new Callback<Map<String, Object>>() {
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
                                } catch (NumberFormatException e) {
                                    Toast.makeText(ManageRoutesActivity.this, "ID tuyến đường không hợp lệ", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        rvRoutes.setAdapter(adapter);
        btnAddRoute.setOnClickListener(v -> startActivity(new Intent(ManageRoutesActivity.this, RouteFormActivity.class)));
        btnRefreshRoutes.setOnClickListener(v -> fetchRoutes());

        fetchRoutes();
    }

    private void fetchRoutes() {
        progressRoutes.setVisibility(View.VISIBLE);
        rvRoutes.setVisibility(View.GONE);
        tvEmptyRoutes.setVisibility(View.GONE);

        // Fetch all routes - the API now returns upstream_trip_count calculated server-side
        // with proper timezone handling (UTC+7 for Vietnam)
        apiService.getRoutes(null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> routesCall, Response<List<Map<String, Object>>> routesResponse) {
                if (!routesResponse.isSuccessful() || routesResponse.body() == null) {
                    progressRoutes.setVisibility(View.GONE);
                    tvEmptyRoutes.setText("Không thể tải tuyến (code=" + routesResponse.code() + ")");
                    tvEmptyRoutes.setVisibility(View.VISIBLE);
                    return;
                }
                // Routes already contain upstream_trip_count from the backend
                List<Map<String, Object>> routes = routesResponse.body();
                updateRoutesList(routes);
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                updateRoutesList(new ArrayList<>());
                tvEmptyRoutes.setText("Lỗi: " + t.getMessage());
                tvEmptyRoutes.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateRoutesList(List<Map<String, Object>> routes) {
        progressRoutes.setVisibility(View.GONE);
        if (routes.isEmpty()) {
            tvEmptyRoutes.setVisibility(View.VISIBLE);
            rvRoutes.setVisibility(View.GONE);
        } else {
            routesList.clear();
            routesList.addAll(routes);
            adapter.notifyDataSetChanged();
            tvEmptyRoutes.setVisibility(View.GONE);
            rvRoutes.setVisibility(View.VISIBLE);
        }
    }
}

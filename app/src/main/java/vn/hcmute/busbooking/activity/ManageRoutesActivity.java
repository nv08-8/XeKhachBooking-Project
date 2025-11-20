package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

public class ManageRoutesActivity extends AppCompatActivity implements RoutesAdapter.RouteItemListener {

    private RecyclerView rvRoutes;
    private FloatingActionButton fabAdd;
    private RoutesAdapter adapter;
    private ApiService apiService;
    private List<Map<String, Object>> routes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routes);

        rvRoutes = findViewById(R.id.rvRoutes);
        fabAdd = findViewById(R.id.fabAddRoute);

        adapter = new RoutesAdapter(routes, this);
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        rvRoutes.setAdapter(adapter);

        apiService = ApiClient.getClient().create(ApiService.class);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ManageRoutesActivity.this, RouteFormActivity.class);
            startActivity(intent);
        });

        loadRoutes();
    }

    private void loadRoutes() {
        Call<List<Map<String, Object>>> call = apiService.getRoutes(null, null, null);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    routes.clear();
                    routes.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ManageRoutesActivity.this, "Không thể tải danh sách tuyến", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(ManageRoutesActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEdit(int position, Map<String, Object> route) {
        // Open RouteFormActivity with extras to edit
        Intent intent = new Intent(this, RouteFormActivity.class);
        intent.putExtra("route_id", (Integer) (Double.valueOf(String.valueOf(route.get("id"))).intValue()));
        startActivity(intent);
    }

    @Override
    public void onDelete(int position, Map<String, Object> route) {
        Object idObj = route.get("id");
        if (idObj == null) return;
        int id = Double.valueOf(String.valueOf(idObj)).intValue();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn xóa tuyến này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Call<Map<String, Object>> call = apiService.deleteRoute(id);
                    call.enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageRoutesActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                                loadRoutes();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadRoutes();
    }
}

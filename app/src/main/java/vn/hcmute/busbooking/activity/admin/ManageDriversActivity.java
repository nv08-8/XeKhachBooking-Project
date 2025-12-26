package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.admin.DriversAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Driver;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageDriversActivity extends AppCompatActivity implements DriversAdapter.OnDriverClickListener {

    private RecyclerView rvDrivers;
    private DriversAdapter adapter;
    private List<Driver> driverList = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;
    private ProgressBar progressDrivers;
    private TextView tvEmptyDrivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_drivers);

        rvDrivers = findViewById(R.id.rvDrivers);
        FloatingActionButton fabAddDriver = findViewById(R.id.fabAddDriver);
        progressDrivers = findViewById(R.id.progressDrivers);
        tvEmptyDrivers = findViewById(R.id.tvEmptyDrivers);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupRecyclerView();
        fetchDrivers();

        fabAddDriver.setOnClickListener(v -> {
            Intent intent = new Intent(ManageDriversActivity.this, DriverFormActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDrivers();
    }

    private void setupRecyclerView() {
        adapter = new DriversAdapter(driverList, this);
        rvDrivers.setLayoutManager(new LinearLayoutManager(this));
        rvDrivers.setAdapter(adapter);
    }

    private void fetchDrivers() {
        progressDrivers.setVisibility(View.VISIBLE);
        rvDrivers.setVisibility(View.GONE);
        tvEmptyDrivers.setVisibility(View.GONE);

        apiService.getAllDrivers(sessionManager.getUserId()).enqueue(new Callback<List<Driver>>() {
            @Override
            public void onResponse(Call<List<Driver>> call, Response<List<Driver>> response) {
                progressDrivers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    driverList.clear();
                    driverList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (driverList.isEmpty()) {
                        tvEmptyDrivers.setVisibility(View.VISIBLE);
                    } else {
                        rvDrivers.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyDrivers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Driver>> call, Throwable t) {
                progressDrivers.setVisibility(View.GONE);
                tvEmptyDrivers.setVisibility(View.VISIBLE);
                Toast.makeText(ManageDriversActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditDriver(Driver driver) {
        Intent intent = new Intent(this, DriverFormActivity.class);
        intent.putExtra("driver_id", driver.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteDriver(Driver driver) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tài xế " + driver.getName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteDriver(driver.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteDriver(int driverId) {
        apiService.deleteDriver(sessionManager.getUserId(), driverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageDriversActivity.this, "Xóa tài xế thành công", Toast.LENGTH_SHORT).show();
                    fetchDrivers(); // Refresh the list
                } else {
                    Toast.makeText(ManageDriversActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ManageDriversActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

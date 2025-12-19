package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.admin.DriversAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Driver;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageDriversActivity extends AppCompatActivity {

    private RecyclerView rvDrivers;
    private DriversAdapter adapter;
    private List<Driver> driverList = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_drivers);

        rvDrivers = findViewById(R.id.rvDrivers);
        FloatingActionButton fabAddDriver = findViewById(R.id.fabAddDriver);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupRecyclerView();
        fetchDrivers();

        fabAddDriver.setOnClickListener(v -> showAddDriverDialog());
    }

    private void setupRecyclerView() {
        adapter = new DriversAdapter(driverList, driver -> {
            // Handle delete driver
        });
        rvDrivers.setLayoutManager(new LinearLayoutManager(this));
        rvDrivers.setAdapter(adapter);
    }

    private void fetchDrivers() {
        apiService.getAllDrivers(sessionManager.getUserId()).enqueue(new Callback<List<Driver>>() {
            @Override
            public void onResponse(Call<List<Driver>> call, Response<List<Driver>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    driverList.clear();
                    driverList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Driver>> call, Throwable t) {
                Toast.makeText(ManageDriversActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDriverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_driver, null);
        builder.setView(dialogView);

        final EditText etDriverName = dialogView.findViewById(R.id.etDriverName);
        final EditText etDriverPhone = dialogView.findViewById(R.id.etDriverPhone);
        final EditText etDriverLicense = dialogView.findViewById(R.id.etDriverLicense);

        builder.setTitle("Thêm tài xế mới");
        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String name = etDriverName.getText().toString();
            String phone = etDriverPhone.getText().toString();
            String license = etDriverLicense.getText().toString();

            Map<String, String> driverData = new HashMap<>();
            driverData.put("name", name);
            driverData.put("phone", phone);
            driverData.put("license_number", license);

            apiService.createDriver(sessionManager.getUserId(), driverData).enqueue(new Callback<Driver>() {
                @Override
                public void onResponse(Call<Driver> call, Response<Driver> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ManageDriversActivity.this, "Thêm tài xế thành công", Toast.LENGTH_SHORT).show();
                        fetchDrivers();
                    } else {
                        Toast.makeText(ManageDriversActivity.this, "Thêm thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Driver> call, Throwable t) {
                    Toast.makeText(ManageDriversActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

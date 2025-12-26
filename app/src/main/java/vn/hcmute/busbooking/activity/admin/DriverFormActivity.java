package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Driver;
import vn.hcmute.busbooking.utils.SessionManager;

public class DriverFormActivity extends AppCompatActivity {

    private EditText etDriverName, etDriverPhone, etDriverLicense;
    private Button btnSaveDriver;

    private ApiService apiService;
    private SessionManager sessionManager;
    private int editingDriverId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        etDriverName = findViewById(R.id.etDriverName);
        etDriverPhone = findViewById(R.id.etDriverPhone);
        etDriverLicense = findViewById(R.id.etDriverLicense);
        btnSaveDriver = findViewById(R.id.btnSaveDriver);

        if (getIntent() != null && getIntent().hasExtra("driver_id")) {
            editingDriverId = getIntent().getIntExtra("driver_id", -1);
            if (editingDriverId != -1) {
                setTitle("Sửa Tài xế");
                loadDriverDetails(editingDriverId);
            } else {
                setTitle("Thêm Tài xế");
            }
        } else {
            setTitle("Thêm Tài xế");
        }

        btnSaveDriver.setOnClickListener(v -> saveDriver());
    }

    private void loadDriverDetails(int driverId) {
        apiService.getDriverById(sessionManager.getUserId(), driverId).enqueue(new Callback<Driver>() {
            @Override
            public void onResponse(Call<Driver> call, Response<Driver> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Driver driver = response.body();
                    etDriverName.setText(driver.getName());
                    etDriverPhone.setText(driver.getPhone());
                    etDriverLicense.setText(driver.getLicenseNumber());
                } else {
                    Toast.makeText(DriverFormActivity.this, "Không thể tải thông tin tài xế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Driver> call, Throwable t) {
                Toast.makeText(DriverFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDriver() {
        String name = etDriverName.getText().toString().trim();
        String phone = etDriverPhone.getText().toString().trim();
        String license = etDriverLicense.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || license.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> driverData = new HashMap<>();
        driverData.put("name", name);
        driverData.put("phone", phone);
        driverData.put("license_number", license);

        Call<Driver> call;
        if (editingDriverId != -1) {
            call = apiService.updateDriver(sessionManager.getUserId(), editingDriverId, driverData);
        } else {
            call = apiService.createDriver(sessionManager.getUserId(), driverData);
        }

        call.enqueue(new Callback<Driver>() {
            @Override
            public void onResponse(Call<Driver> call, Response<Driver> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DriverFormActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(DriverFormActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Driver> call, Throwable t) {
                Toast.makeText(DriverFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

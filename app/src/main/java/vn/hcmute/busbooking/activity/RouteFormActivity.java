package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class RouteFormActivity extends AppCompatActivity {

    private EditText edtOrigin, edtDestination, edtPrice, edtDuration;
    private Button btnSave;
    private ApiService apiService;
    private Integer editingRouteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_form);

        edtOrigin = findViewById(R.id.edtOrigin);
        edtDestination = findViewById(R.id.edtDestination);
        edtPrice = findViewById(R.id.edtPrice);
        edtDuration = findViewById(R.id.edtDuration);
        btnSave = findViewById(R.id.btnSaveRoute);

        apiService = ApiClient.getClient().create(ApiService.class);

        if (getIntent() != null && getIntent().hasExtra("route_id")) {
            editingRouteId = getIntent().getIntExtra("route_id", -1);
            if (editingRouteId != -1) {
                loadRouteDetails(editingRouteId);
            }
        }

        btnSave.setOnClickListener(v -> saveRoute());
    }

    private void loadRouteDetails(int id) {
        Call<Map<String, Object>> call = apiService.getRouteById(id);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    Object origin = data.get("origin");
                    Object destination = data.get("destination");
                    Object price = data.get("base_price");
                    Object duration = data.get("duration_min");
                    if (origin != null) edtOrigin.setText(String.valueOf(origin));
                    if (destination != null) edtDestination.setText(String.valueOf(destination));
                    if (price != null) edtPrice.setText(String.valueOf(price));
                    if (duration != null) edtDuration.setText(String.valueOf(duration));
                } else {
                    Toast.makeText(RouteFormActivity.this, "Không tải được thông tin tuyến", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RouteFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRoute() {
        String origin = edtOrigin.getText().toString().trim();
        String destination = edtDestination.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String durationStr = edtDuration.getText().toString().trim();

        if (TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin (tối thiểu: nơi đi, nơi đến, giá)", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("origin", origin);
        body.put("destination", destination);
        try {
            body.put("base_price", Integer.parseInt(priceStr));
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Giá phải là số", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(durationStr)) {
            try {
                body.put("duration_min", Integer.parseInt(durationStr));
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        Call<Map<String, Object>> call;
        if (editingRouteId == null) {
            call = apiService.createRoute(body);
        } else {
            call = apiService.updateRoute(editingRouteId, body);
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RouteFormActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RouteFormActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RouteFormActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

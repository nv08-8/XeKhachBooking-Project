package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripFormActivity extends AppCompatActivity {

    private Spinner spinnerRoute;
    private EditText etOperator, etBusType, etDepartureTime, etArrivalTime, etPrice, etSeatsTotal;
    private Button btnSaveTrip;

    private ApiService apiService;
    private SessionManager sessionManager;
    private int currentTripId = -1; // -1 for new, > -1 for editing
    private List<Map<String, Object>> routesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        spinnerRoute = findViewById(R.id.spinnerRoute);
        etOperator = findViewById(R.id.etOperator);
        etBusType = findViewById(R.id.etBusType);
        etDepartureTime = findViewById(R.id.etDepartureTime);
        etArrivalTime = findViewById(R.id.etArrivalTime);
        etPrice = findViewById(R.id.etPrice);
        etSeatsTotal = findViewById(R.id.etSeatsTotal);
        btnSaveTrip = findViewById(R.id.btnSaveTrip);

        loadRoutes();

        if (getIntent().hasExtra("trip_id")) {
            currentTripId = getIntent().getIntExtra("trip_id", -1);
            if (currentTripId != -1) {
                setTitle("Sửa chuyến đi");
                loadTripDetails(currentTripId);
            } else {
                setTitle("Thêm chuyến đi");
            }
        } else {
            setTitle("Thêm chuyến đi");
        }

        btnSaveTrip.setOnClickListener(v -> saveTrip());
    }

    private void loadRoutes() {
        apiService.getRoutes(null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    routesList.clear();
                    routesList.addAll(response.body());
                    List<String> routeNames = new ArrayList<>();
                    for (Map<String, Object> route : routesList) {
                        routeNames.add(route.get("origin") + " - " + route.get("destination"));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(TripFormActivity.this,
                            android.R.layout.simple_spinner_item, routeNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRoute.setAdapter(adapter);

                    // If editing, load details after routes are loaded to set spinner correctly
                    if (currentTripId != -1) {
                        loadTripDetails(currentTripId);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(TripFormActivity.this, "Không thể tải danh sách tuyến", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTripDetails(int tripId) {
        apiService.getTripDetails(tripId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    // The actual trip data is nested inside the "data" key
                    if (responseData.get("data") instanceof Map) {
                        Map<String, Object> trip = (Map<String, Object>) responseData.get("data");

                        etOperator.setText(String.valueOf(trip.get("operator")));
                        etBusType.setText(String.valueOf(trip.get("bus_type")));
                        etDepartureTime.setText(String.valueOf(trip.get("departure_time")));
                        etArrivalTime.setText(String.valueOf(trip.get("arrival_time")));
                        etPrice.setText(String.valueOf(trip.get("price")));
                        etSeatsTotal.setText(String.valueOf(trip.get("seats_total")));

                        // Set spinner selection
                        Object routeIdObj = trip.get("route_id");
                        if (routeIdObj instanceof Number) {
                            int routeId = ((Number) routeIdObj).intValue();
                            for (int i = 0; i < routesList.size(); i++) {
                                Object idObj = routesList.get(i).get("id");
                                if (idObj instanceof String) { // IDs from API are strings
                                    if (Integer.parseInt((String) idObj) == routeId) {
                                        spinnerRoute.setSelection(i);
                                        break;
                                    }
                                } else if (idObj instanceof Number) {
                                     if (((Number) idObj).intValue() == routeId) {
                                        spinnerRoute.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(TripFormActivity.this, "Không thể tải thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTrip() {
        int selectedRoutePosition = spinnerRoute.getSelectedItemPosition();
        if (selectedRoutePosition < 0 || selectedRoutePosition >= routesList.size()) {
            Toast.makeText(this, "Vui lòng chọn tuyến đường", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> selectedRoute = routesList.get(selectedRoutePosition);
        int routeId = -1;
        Object idObj = selectedRoute.get("id");
         if (idObj instanceof String) { 
            routeId = Integer.parseInt((String) idObj);
        } else if (idObj instanceof Number) {
            routeId = ((Number) idObj).intValue();
        }
        
        if(routeId == -1) {
             Toast.makeText(this, "ID tuyến đường không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("route_id", routeId);
        tripData.put("operator", etOperator.getText().toString());
        tripData.put("bus_type", etBusType.getText().toString());
        tripData.put("departure_time", etDepartureTime.getText().toString());
        tripData.put("arrival_time", etArrivalTime.getText().toString());
        tripData.put("price", Double.parseDouble(etPrice.getText().toString()));
        tripData.put("seats_total", Integer.parseInt(etSeatsTotal.getText().toString()));

        int userId = sessionManager.getUserId();

        Call<Map<String, Object>> call;
        if (currentTripId != -1) {
            // Update existing trip
            call = apiService.updateTrip(userId, currentTripId, tripData);
        } else {
            // Create new trip
            call = apiService.createTrip(userId, tripData);
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TripFormActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(TripFormActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(TripFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

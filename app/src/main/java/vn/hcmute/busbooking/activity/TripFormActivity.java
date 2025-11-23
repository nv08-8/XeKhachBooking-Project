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
    private Trip currentTrip;
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

        if (getIntent().hasExtra("trip")) {
            currentTrip = (Trip) getIntent().getSerializableExtra("trip");
            populateTripData();
        }

        btnSaveTrip.setOnClickListener(v -> saveTrip());
    }

    private void loadRoutes() {
        Call<List<Map<String, Object>>> call = apiService.getRoutes(null, null, null);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
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

                    if (currentTrip != null) {
                        // Set spinner selection for editing
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Handle failure
            }
        });
    }

    private void populateTripData() {
        if (currentTrip != null) {
            etOperator.setText(currentTrip.getOperator());
            etBusType.setText(currentTrip.getBusType());
            etDepartureTime.setText(currentTrip.getDepartureTime());
            etArrivalTime.setText(currentTrip.getArrivalTime());
            etPrice.setText(String.valueOf(currentTrip.getPrice()));
            etSeatsTotal.setText(String.valueOf(currentTrip.getSeatsTotal()));
        }
    }

    private void saveTrip() {
        // TODO: Implement save logic
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

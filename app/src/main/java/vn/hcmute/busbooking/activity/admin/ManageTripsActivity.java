package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.TripFormActivity;
import vn.hcmute.busbooking.activity.admin.AdminAddBookingActivity;
import vn.hcmute.busbooking.adapter.admin.TripsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageTripsActivity extends AppCompatActivity implements TripsAdapter.OnTripClickListener {

    private RecyclerView rvTrips;
    private ProgressBar progressTrips;
    private TextView tvEmptyTrips;
    private Button btnAddTrip, btnRefreshTrips;
    private Spinner spinnerOperatorFilter;
    private TripsAdapter adapter;
    private List<Map<String, Object>> tripList = new ArrayList<>();
    private List<String> operatorList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;
    private int routeId = -1;
    private String selectedOperator = "Tất cả nhà xe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_trips);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        rvTrips = findViewById(R.id.rvTrips);
        progressTrips = findViewById(R.id.progressTrips);
        tvEmptyTrips = findViewById(R.id.tvEmptyTrips);
        btnAddTrip = findViewById(R.id.btnAddTrip);
        btnRefreshTrips = findViewById(R.id.btnRefreshTrips);
        spinnerOperatorFilter = findViewById(R.id.spinnerOperatorFilter);

        routeId = getIntent().getIntExtra("route_id", -1);

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripsAdapter(tripList, this, this);
        rvTrips.setAdapter(adapter);

        btnAddTrip.setOnClickListener(v -> {
            Intent intent = new Intent(ManageTripsActivity.this, TripFormActivity.class);
            if (routeId != -1) {
                intent.putExtra("route_id", routeId);
            }
            startActivity(intent);
        });

        btnRefreshTrips.setOnClickListener(v -> {
            fetchOperators();
            fetchTrips(routeId, selectedOperator);
        });

        setupFilterListeners();
        fetchOperators();
        fetchTrips(routeId, selectedOperator);
    }

    private void setupFilterListeners() {
        spinnerOperatorFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOperator = operatorList.get(position);
                fetchTrips(routeId, selectedOperator);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchOperators() {
        apiService.getOperators().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    operatorList.clear();
                    operatorList.add("Tất cả nhà xe");
                    operatorList.addAll(response.body());

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(ManageTripsActivity.this,
                            android.R.layout.simple_spinner_item, operatorList);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerOperatorFilter.setAdapter(spinnerAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void fetchTrips(int routeId, String operator) {
        progressTrips.setVisibility(View.VISIBLE);
        tvEmptyTrips.setVisibility(View.GONE);
        rvTrips.setVisibility(View.GONE);

        Integer routeIdParam = (routeId != -1) ? routeId : null;
        
        // Use getTrips(routeId, origin, destination, date) but it doesn't support operator directly in current ApiService
        // I need to check if ApiService has a method that supports operator or filter manually
        Call<List<Map<String, Object>>> call = apiService.getTrips(routeIdParam, null, null, null);

        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressTrips.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> allTrips = response.body();
                    List<Map<String, Object>> filteredTrips = new ArrayList<>();

                    for (Map<String, Object> trip : allTrips) {
                        String tripOperator = (String) trip.get("operator");
                        if (operator.equals("Tất cả nhà xe") || (tripOperator != null && tripOperator.equals(operator))) {
                            filteredTrips.add(trip);
                        }
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    Collections.sort(filteredTrips, (t1, t2) -> {
                        try {
                            Date d1 = sdf.parse((String) t1.get("departure_time"));
                            Date d2 = sdf.parse((String) t2.get("departure_time"));
                            return d2.compareTo(d1);
                        } catch (ParseException | NullPointerException e) {
                            return 0;
                        }
                    });

                    tripList.clear();
                    tripList.addAll(filteredTrips);
                    adapter.notifyDataSetChanged();

                    if (tripList.isEmpty()) {
                        tvEmptyTrips.setVisibility(View.VISIBLE);
                    } else {
                        rvTrips.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyTrips.setText("Không thể tải danh sách chuyến đi");
                    tvEmptyTrips.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressTrips.setVisibility(View.GONE);
                tvEmptyTrips.setText("Lỗi: " + t.getMessage());
                tvEmptyTrips.setVisibility(View.VISIBLE);
            }
        });
    }

    private int getTripIdFromMap(Map<String, Object> trip) {
        Object idObj = trip.get("trip_id");
        if (idObj == null) {
            idObj = trip.get("id");
        }

        if (idObj == null) {
            return -1;
        }

        try {
            return (int) Double.parseDouble(idObj.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void onEditTrip(Map<String, Object> trip) {
        int tripId = getTripIdFromMap(trip);
        if (tripId != -1) {
            Intent intent = new Intent(this, TripFormActivity.class);
            intent.putExtra("trip_id", tripId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "ID chuyến đi không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteTrip(Map<String, Object> trip) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chuyến đi này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int tripId = getTripIdFromMap(trip);
                    if (tripId != -1) {
                        int adminId = sessionManager.getUserId();
                        Call<Map<String, Object>> call = apiService.deleteTrip(adminId, tripId);
                        call.enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(ManageTripsActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                                    fetchTrips(routeId, selectedOperator);
                                } else {
                                    Toast.makeText(ManageTripsActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Toast.makeText(ManageTripsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "ID chuyến đi không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onManageSeats(Map<String, Object> trip) {
        int tripId = getTripIdFromMap(trip);
        if (tripId != -1) {
            Intent intent = new Intent(this, AdminAddBookingActivity.class);
            intent.putExtra("trip_id", tripId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "ID chuyến đi không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;

public class TripListActivity extends AppCompatActivity {

    private static final String TAG = "TripListActivity";

    private TextView tvRoute, tvDate;
    private Spinner spinnerPriceFilter, spinnerSeatTypeFilter, spinnerTimeFilter;
    private RecyclerView rvTrips;
    private TripAdapter tripAdapter;
    private List<Trip> allTrips = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);

        tvRoute = findViewById(R.id.tvRoute);
        tvDate = findViewById(R.id.tvDate);
        spinnerPriceFilter = findViewById(R.id.spinnerPriceFilter);
        spinnerSeatTypeFilter = findViewById(R.id.spinnerSeatTypeFilter);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);
        rvTrips = findViewById(R.id.rvTrips);

        // Get data from intent
        String from = getIntent().getStringExtra("origin");
        String to = getIntent().getStringExtra("destination");
        String date = getIntent().getStringExtra("date");

        if (from == null) from = "";
        if (to == null) to = "";
        if (date == null) date = "Hôm nay";

        tvRoute.setText(from + " → " + to);
        tvDate.setText("Ngày: " + date);

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(new ArrayList<>());
        rvTrips.setAdapter(tripAdapter);

        tripAdapter.setOnItemClickListener(trip -> {
            Intent intent = new Intent(TripListActivity.this, SeatSelectionActivity.class);
            intent.putExtra("trip", trip);
            startActivity(intent);
        });

        apiService = ApiClient.getClient().create(ApiService.class);
        fetchTrips(from, to);

        setupFilters();
    }

    private void fetchTrips(String from, String to) {
        apiService.getTrips(from, to).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTrips = response.body();
                    applyFilters();
                    Log.d(TAG, "Trips loaded: " + allTrips.size());
                } else {
                    Log.e(TAG, "Response not successful. Code: " + response.code());
                    Toast.makeText(TripListActivity.this, "Không tìm thấy chuyến đi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(TripListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilters() {
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerPriceFilter.setOnItemSelectedListener(filterListener);
        spinnerSeatTypeFilter.setOnItemSelectedListener(filterListener);
        spinnerTimeFilter.setOnItemSelectedListener(filterListener);
    }

    private void applyFilters() {
        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : allTrips) {
            if (checkPriceFilter(trip) && checkSeatTypeFilter(trip) && checkTimeFilter(trip)) {
                filteredTrips.add(trip);
            }
        }
        tripAdapter.updateTrips(filteredTrips);
    }

    private boolean checkPriceFilter(Trip trip) {
        String selectedFilter = spinnerPriceFilter.getSelectedItem().toString();
        if (selectedFilter.contains("Tất cả")) return true;

        double price = trip.getPrice();
        if (selectedFilter.contains("Dưới 200k")) return price < 200000;
        if (selectedFilter.contains("200k - 400k")) return price >= 200000 && price <= 400000;
        if (selectedFilter.contains("Trên 400k")) return price > 400000;

        return true;
    }

    private boolean checkSeatTypeFilter(Trip trip) {
        String selectedFilter = spinnerSeatTypeFilter.getSelectedItem().toString();
        if (selectedFilter.contains("Tất cả")) return true;

        String seatType = trip.getBusType();
        if (seatType == null) return false;
        return selectedFilter.contains(seatType);
    }

    private boolean checkTimeFilter(Trip trip) {
        String selectedFilter = spinnerTimeFilter.getSelectedItem().toString();
        if (selectedFilter.contains("Tất cả")) return true;

        String departureTime = trip.getDepartureTime();
        if (departureTime == null) return false;

        try {
            int hour = Integer.parseInt(departureTime.split(":")[0]);
            if (selectedFilter.contains("Sáng")) return hour >= 6 && hour < 12;
            if (selectedFilter.contains("Chiều")) return hour >= 12 && hour < 18;
            if (selectedFilter.contains("Tối")) return hour >= 18 && hour < 24;
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Trip;

public class SelectPickupPointActivity extends AppCompatActivity {

    private Trip trip;
    private ArrayList<String> seatLabels;
    private ListView lvPickupLocations;
    private Button btnContinue;
    private List<Location> pickupLocations;
    private Location selectedPickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pickup_point);

        // Get data from previous activity
        trip = getIntent().getParcelableExtra("trip");
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");

        if (trip == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin chuyến đi.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        lvPickupLocations = findViewById(R.id.lvPickupLocations);
        btnContinue = findViewById(R.id.btnContinue);

        // Fetch locations
        fetchPickupLocations();

        // Handle list item clicks
        lvPickupLocations.setOnItemClickListener((parent, view, position, id) -> selectedPickup = pickupLocations.get(position));

        // Handle continue button click
        btnContinue.setOnClickListener(v -> {
            if (selectedPickup == null) {
                Toast.makeText(this, "Vui lòng chọn điểm đón.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SelectDropoffPointActivity.class);
            intent.putExtra("trip", trip);
            intent.putStringArrayListExtra("seat_labels", seatLabels);
            intent.putExtra("pickup_location", selectedPickup);
            startActivity(intent);
        });
    }

    private void fetchPickupLocations() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getPickupLocations(trip.getId()).enqueue(new Callback<List<Location>>() {
            @Override
            public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Filter for pickup points
                    pickupLocations = response.body().stream()
                            .filter(location -> "pickup".equalsIgnoreCase(location.getType()) || "both".equalsIgnoreCase(location.getType()))
                            .collect(Collectors.toList());

                    List<String> pickupNames = new ArrayList<>();
                    for (Location location : pickupLocations) {
                        pickupNames.add(formatLocationName(location));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(SelectPickupPointActivity.this,
                            android.R.layout.simple_list_item_single_choice, pickupNames);
                    lvPickupLocations.setAdapter(adapter);
                    lvPickupLocations.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                } else {
                    Toast.makeText(SelectPickupPointActivity.this, "Không thể tải danh sách điểm đón.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Location>> call, Throwable t) {
                Toast.makeText(SelectPickupPointActivity.this, "Lỗi mạng.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String formatLocationName(Location location) {
        String name = location.getName();
        String address = location.getAddress();

        if (name != null && !name.isEmpty() && address != null && !address.isEmpty()) {
            return name + " - " + address;
        } else if (name != null && !name.isEmpty()) {
            return name;
        } else if (address != null && !address.isEmpty()) {
            return address;
        } else {
            return "Điểm dừng không tên";
        }
    }
}

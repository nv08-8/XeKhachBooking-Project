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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Trip;

public class SelectDropoffPointActivity extends AppCompatActivity {

    private Trip trip;
    private ArrayList<String> seatLabels;
    private Location selectedPickup;
    private ListView lvDropoffLocations;
    private Button btnContinue;
    private List<Location> dropoffLocations;
    private Location selectedDropoff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dropoff_point);

        // Get data from previous activity
        trip = getIntent().getParcelableExtra("trip");
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");
        selectedPickup = getIntent().getParcelableExtra("pickup_location");

        if (trip == null || selectedPickup == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin chuyến đi hoặc điểm đón.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        lvDropoffLocations = findViewById(R.id.lvDropoffLocations);
        btnContinue = findViewById(R.id.btnContinue);

        // Fetch locations
        fetchDropoffLocations();

        // Handle list item clicks
        lvDropoffLocations.setOnItemClickListener((parent, view, position, id) -> selectedDropoff = dropoffLocations.get(position));

        // Handle continue button click
        btnContinue.setOnClickListener(v -> {
            if (selectedDropoff == null) {
                Toast.makeText(this, "Vui lòng chọn điểm trả.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("trip", trip);
            intent.putStringArrayListExtra("seat_labels", seatLabels);
            intent.putExtra("pickup_location", selectedPickup);
            intent.putExtra("dropoff_location", selectedDropoff);
            startActivity(intent);
        });
    }

    private void fetchDropoffLocations() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getDropoffLocations(trip.getId()).enqueue(new Callback<List<Location>>() {
            @Override
            public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dropoffLocations = response.body();
                    List<String> dropoffNames = new ArrayList<>();
                    for (Location location : dropoffLocations) {
                        dropoffNames.add(location.getName() + " - " + location.getAddress());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(SelectDropoffPointActivity.this,
                            android.R.layout.simple_list_item_single_choice, dropoffNames);
                    lvDropoffLocations.setAdapter(adapter);
                    lvDropoffLocations.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                } else {
                    Toast.makeText(SelectDropoffPointActivity.this, "Không thể tải danh sách điểm trả.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Location>> call, Throwable t) {
                Toast.makeText(SelectDropoffPointActivity.this, "Lỗi mạng.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

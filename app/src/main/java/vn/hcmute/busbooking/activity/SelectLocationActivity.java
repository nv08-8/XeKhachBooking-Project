package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.LocationAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Location;

public class SelectLocationActivity extends AppCompatActivity implements LocationAdapter.OnLocationClickListener {

    public static final String EXTRA_LOCATION_TYPE = "location_type";
    public static final String EXTRA_TRIP_ID = "trip_id";
    public static final String EXTRA_SELECTED_LOCATION = "selected_location";
    public static final String LOCATION_TYPE_PICKUP = "pickup";
    public static final String LOCATION_TYPE_DROPOFF = "dropoff";

    private RecyclerView rvLocations;
    private LocationAdapter locationAdapter;
    private List<Location> locationList = new ArrayList<>();
    private Location selectedLocation;
    private MaterialButton btnListView, btnMapView;
    private ApiService apiService;
    private int tripId;
    private String locationType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        locationType = getIntent().getStringExtra(EXTRA_LOCATION_TYPE);
        tripId = getIntent().getIntExtra(EXTRA_TRIP_ID, -1);
        apiService = ApiClient.getClient().create(ApiService.class);


        if (LOCATION_TYPE_PICKUP.equals(locationType)) {
            setContentView(R.layout.activity_select_pickup_point);
        } else {
            setContentView(R.layout.activity_select_dropoff_point);
        }

        View mainView = findViewById(R.id.main);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            appBarLayout.setPadding(0, topInset, 0, 0);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        rvLocations = findViewById(R.id.rvLocations);
        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        btnListView = findViewById(R.id.btnListView);
        btnMapView = findViewById(R.id.btnMapView);
        locationAdapter = new LocationAdapter(this, locationList, this);
        rvLocations.setAdapter(locationAdapter);


        loadLocations();

        btnListView.setOnClickListener(v -> setActiveButton(btnListView));
        btnMapView.setOnClickListener(v -> setActiveButton(btnMapView));

        // Set initial active button
        setActiveButton(btnListView);

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SELECTED_LOCATION, selectedLocation);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn một điểm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLocations() {
        if (tripId == -1) {
            Toast.makeText(this, "Trip ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<Location>> call;
        if (LOCATION_TYPE_PICKUP.equals(locationType)) {
            call = apiService.getPickupLocations(tripId);
        } else {
            call = apiService.getDropoffLocations(tripId);
        }

        call.enqueue(new Callback<List<Location>>() {
            @Override
            public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    locationList.clear();
                    locationList.addAll(response.body());
                    locationAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SelectLocationActivity.this, "Không thể tải danh sách điểm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Location>> call, Throwable t) {
                Toast.makeText(SelectLocationActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setActiveButton(MaterialButton activeButton) {
        btnListView.setSelected(btnListView == activeButton);
        btnMapView.setSelected(btnMapView == activeButton);

        updateButtonStyles();
    }

    private void updateButtonStyles() {
        // Active style
        ColorStateList activeBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E3F2FD"));
        int activeTextColor = Color.parseColor("#007AFF");

        // Inactive style
        ColorStateList inactiveBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT);
        int inactiveTextColor = Color.DKGRAY;

        if (btnListView.isSelected()) {
            btnListView.setBackgroundTintList(activeBackgroundColor);
            btnListView.setTextColor(activeTextColor);
            btnListView.setIconTint(ColorStateList.valueOf(activeTextColor));
        } else {
            btnListView.setBackgroundTintList(inactiveBackgroundColor);
            btnListView.setTextColor(inactiveTextColor);
            btnListView.setIconTint(ColorStateList.valueOf(inactiveTextColor));
        }

        if (btnMapView.isSelected()) {
            btnMapView.setBackgroundTintList(activeBackgroundColor);
            btnMapView.setTextColor(activeTextColor);
            btnMapView.setIconTint(ColorStateList.valueOf(activeTextColor));
        } else {
            btnMapView.setBackgroundTintList(inactiveBackgroundColor);
            btnMapView.setTextColor(inactiveTextColor);
            btnMapView.setIconTint(ColorStateList.valueOf(inactiveTextColor));
        }
    }

    @Override
    public void onLocationClick(Location location) {
        selectedLocation = location;
        locationAdapter.setSelectedLocation(location);

    }
}

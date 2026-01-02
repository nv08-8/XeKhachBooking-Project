package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import vn.hcmute.busbooking.R;

public class SelectLocationMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLatLng;
    private String selectedAddress = "";
    private TextView tvSelectedAddress;
    private Button btnConfirm, btnCancel;
    private Geocoder geocoder;

    // Default location (Hanoi, Vietnam)
    private static final LatLng DEFAULT_LOCATION = new LatLng(21.028511, 105.804817);
    private static final int DEFAULT_ZOOM = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location_map);

        // Setup toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        tvSelectedAddress = findViewById(R.id.selectedAddress);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        geocoder = new Geocoder(this, Locale.getDefault());

        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Handle buttons
        btnCancel.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Move camera to default location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        // Handle map click
        mMap.setOnMapClickListener(this::handleMapClick);

        // Optional: Enable current location button if you have location permission
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            // No location permission
        }
    }

    private void handleMapClick(LatLng latLng) {
        // Remove previous marker
        if (selectedMarker != null) {
            selectedMarker.remove();
        }

        // Add new marker
        selectedMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Địa điểm đã chọn"));

        selectedLatLng = latLng;

        // Reverse geocode to get address
        resolveAddress(latLng);

        // Enable confirm button
        btnConfirm.setEnabled(true);
    }

    private void resolveAddress(LatLng latLng) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        latLng.latitude,
                        latLng.longitude,
                        1
                );

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    selectedAddress = address.getAddressLine(0) != null
                            ? address.getAddressLine(0)
                            : "Không xác định";
                } else {
                    selectedAddress = String.format(Locale.getDefault(),
                            "Lat: %.4f, Lng: %.4f",
                            latLng.latitude,
                            latLng.longitude);
                }

                runOnUiThread(() -> tvSelectedAddress.setText(selectedAddress));
            } catch (IOException e) {
                selectedAddress = String.format(Locale.getDefault(),
                        "Lat: %.4f, Lng: %.4f",
                        latLng.latitude,
                        latLng.longitude);
                runOnUiThread(() -> tvSelectedAddress.setText(selectedAddress));
            }
        }).start();
    }

    private void confirmSelection() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Vui lòng chọn địa điểm trên bản đồ.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_lat", selectedLatLng.latitude);
        resultIntent.putExtra("selected_lng", selectedLatLng.longitude);
        resultIntent.putExtra("selected_address", selectedAddress);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}


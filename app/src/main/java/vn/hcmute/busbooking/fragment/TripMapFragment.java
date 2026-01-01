package vn.hcmute.busbooking.fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Location;

public class TripMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "TripMapFragment";
    private static final String ARG_PICKUP_LOCATIONS = "pickup_locations";
    private static final String ARG_DROPOFF_LOCATIONS = "dropoff_locations";

    private GoogleMap mMap;
    private List<Location> pickupLocations = new ArrayList<>();
    private List<Location> dropoffLocations = new ArrayList<>();

    public TripMapFragment() {
    }

    public static TripMapFragment newInstance(List<Location> pickups, List<Location> dropoffs) {
        TripMapFragment fragment = new TripMapFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_PICKUP_LOCATIONS, new ArrayList<>(pickups));
        args.putParcelableArrayList(ARG_DROPOFF_LOCATIONS, new ArrayList<>(dropoffs));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pickupLocations = getArguments().getParcelableArrayList(ARG_PICKUP_LOCATIONS);
            dropoffLocations = getArguments().getParcelableArrayList(ARG_DROPOFF_LOCATIONS);
        }
        if (pickupLocations == null) pickupLocations = new ArrayList<>();
        if (dropoffLocations == null) dropoffLocations = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");

        if (pickupLocations.isEmpty() && dropoffLocations.isEmpty()) {
            Log.w(TAG, "No pickup or dropoff locations to display");
            // Set default location (center of Vietnam)
            LatLng vietnamCenter = new LatLng(10.8231, 106.6797);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 6));
            return;
        }

        // Add markers for pickup locations (blue/green)
        for (Location location : pickupLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                BitmapDescriptor markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(location.getName())
                        .snippet(location.getAddress())
                        .icon(markerIcon));
                Log.d(TAG, "Added pickup marker: " + location.getName());
            }
        }

        // Add markers for dropoff locations (red)
        for (Location location : dropoffLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                BitmapDescriptor markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(location.getName())
                        .snippet(location.getAddress())
                        .icon(markerIcon));
                Log.d(TAG, "Added dropoff marker: " + location.getName());
            }
        }

        // Adjust camera to fit all markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasValidLocations = false;

        for (Location location : pickupLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
                hasValidLocations = true;
            }
        }

        for (Location location : dropoffLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
                hasValidLocations = true;
            }
        }

        if (hasValidLocations) {
            try {
                LatLngBounds bounds = builder.build();
                int padding = 100; // offset from edges
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                Log.d(TAG, "Camera adjusted to fit all markers");
            } catch (Exception e) {
                Log.e(TAG, "Error adjusting camera: " + e.getMessage());
                // Fallback: zoom to first valid location
                if (!pickupLocations.isEmpty() && pickupLocations.get(0).getLatitude() != 0) {
                    LatLng firstLoc = new LatLng(pickupLocations.get(0).getLatitude(),
                                                  pickupLocations.get(0).getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLoc, 12));
                }
            }
        }

        // Optional: Draw polyline connecting pickup and dropoff locations
        drawRoute();
    }

    private void drawRoute() {
        if (pickupLocations.isEmpty() || dropoffLocations.isEmpty()) {
            return;
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.BLUE);
        polylineOptions.width(5);
        polylineOptions.geodesic(true);

        // Add pickup locations to polyline
        for (Location location : pickupLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        }

        // Add dropoff locations to polyline
        for (Location location : dropoffLocations) {
            if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        }

        if (polylineOptions.getPoints().size() > 1) {
            mMap.addPolyline(polylineOptions);
            Log.d(TAG, "Route polyline drawn with " + polylineOptions.getPoints().size() + " points");
        }
    }

    public void updateLocations(List<Location> pickups, List<Location> dropoffs) {
        this.pickupLocations = pickups != null ? pickups : new ArrayList<>();
        this.dropoffLocations = dropoffs != null ? dropoffs : new ArrayList<>();

        if (mMap != null) {
            mMap.clear();
            onMapReady(mMap);
        }
    }
}


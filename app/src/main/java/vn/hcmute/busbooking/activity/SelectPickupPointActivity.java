package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.util.CurrencyUtil;

public class SelectPickupPointActivity extends AppCompatActivity {

    private Trip trip;
    private ArrayList<String> seatLabels;
    private RecyclerView rvLocations;
    private Button btnContinue;
    private TextView tvSubtotal;
    private List<Location> pickupLocations;
    private Location selectedPickup;
    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pickup_point);

        // Setup toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get data from previous activity
        trip = getIntent().getParcelableExtra("trip");
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");

        if (trip == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin chuyến đi.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        rvLocations = findViewById(R.id.rvLocations);
        btnContinue = findViewById(R.id.btnContinue);
        tvSubtotal = findViewById(R.id.tvSubtotal);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(new ArrayList<>(), loc -> selectedPickup = loc);
        rvLocations.setAdapter(adapter);

        // Display subtotal
        if (seatLabels != null && trip != null) {
            double totalAmount = seatLabels.size() * trip.getPrice();
            tvSubtotal.setText(CurrencyUtil.formatVND(totalAmount));
        }

        // Fetch locations
        fetchPickupLocations();

        // RecyclerView adapter handles item clicks and selection

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
                    // Filter for pickup points (avoid using Stream API for minSdk compatibility)
                    pickupLocations = new ArrayList<>();
                    for (Location location : response.body()) {
                        String type = location.getType();
                        if (type == null) continue;
                        if ("pickup".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
                            pickupLocations.add(location);
                        }
                    }

                    adapter.setItems(pickupLocations);
                    adapter.notifyDataSetChanged();
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

    // Simple RecyclerView adapter for Location items with single-selection support
    private static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.VH> {
        interface OnClick { void onClick(Location loc); }
        private final List<Location> items;
        private final OnClick onClick;
        private int selected = -1;

        LocationAdapter(List<Location> items, OnClick onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        void setItems(List<Location> newItems) {
            items.clear();
            items.addAll(newItems);
            selected = -1;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_activated_1, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Location loc = items.get(pos);
            String name = loc.getName();
            String addr = loc.getAddress();
            holder.text.setText((name != null && !name.isEmpty() ? name : (addr != null ? addr : "Không tên")));
            holder.itemView.setActivated(pos == selected);
            holder.itemView.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                int old = selected;
                selected = p;
                notifyItemChanged(old);
                notifyItemChanged(selected);
                if (onClick != null) onClick.onClick(items.get(p));
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView text;
            VH(View itemView) {
                super(itemView);
                text = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}

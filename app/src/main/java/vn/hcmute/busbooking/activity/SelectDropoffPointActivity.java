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
import java.util.stream.Collectors;

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
    private RecyclerView rvLocations;
    private Button btnContinue;
    private TextView tvSubtotal;
    private List<Location> dropoffLocations;
    private Location selectedDropoff;
    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dropoff_point);

        // Get data from previous activity
        trip = getIntent().getParcelableExtra("trip");
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");
        selectedPickup = getIntent().getParcelableExtra("pickup_location");

        if (trip == null || selectedPickup == null || seatLabels == null || seatLabels.isEmpty()) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin chuyến đi, điểm đón hoặc ghế đã chọn.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        rvLocations = findViewById(R.id.rvLocations);
        btnContinue = findViewById(R.id.btnContinue);
        tvSubtotal = findViewById(R.id.tvSubtotal);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(new ArrayList<>(), loc -> selectedDropoff = loc);
        rvLocations.setAdapter(adapter);

        // Display subtotal
        if (seatLabels != null && trip != null) {
            double totalAmount = seatLabels.size() * trip.getPrice();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvSubtotal.setText(formatter.format(totalAmount));
        }

        // Fetch locations
        fetchDropoffLocations();

        // Handle continue button click
        btnContinue.setOnClickListener(v -> {
            android.util.Log.d("SelectDropoff", "Continue button clicked");

            if (selectedDropoff == null) {
                android.util.Log.e("SelectDropoff", "No dropoff location selected");
                Toast.makeText(this, "Vui lòng chọn điểm trả.", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("SelectDropoff", "Creating intent to ContactInfoActivity");
            android.util.Log.d("SelectDropoff", "Trip: " + (trip != null ? trip.getId() : "null"));
            android.util.Log.d("SelectDropoff", "Seats: " + (seatLabels != null ? seatLabels.size() : "null"));
            android.util.Log.d("SelectDropoff", "Pickup: " + (selectedPickup != null ? selectedPickup.getName() : "null"));
            android.util.Log.d("SelectDropoff", "Dropoff: " + (selectedDropoff != null ? selectedDropoff.getName() : "null"));

            try {
                // Go to contact info screen before payment
                Intent intent = new Intent(this, ContactInfoActivity.class);
                intent.putExtra("trip", trip);
                intent.putStringArrayListExtra("seat_labels", seatLabels);
                intent.putExtra("pickup_location", selectedPickup);
                intent.putExtra("dropoff_location", selectedDropoff);

                android.util.Log.d("SelectDropoff", "Starting ContactInfoActivity...");
                startActivity(intent);
                android.util.Log.d("SelectDropoff", "ContactInfoActivity started successfully");
            } catch (Exception e) {
                android.util.Log.e("SelectDropoff", "Error starting ContactInfoActivity", e);
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchDropoffLocations() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getDropoffLocations(trip.getId()).enqueue(new Callback<List<Location>>() {
            @Override
            public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Filter for dropoff points
                    dropoffLocations = new ArrayList<>();
                    for (Location location : response.body()) {
                        String type = location.getType();
                        if (type == null) continue;
                        if ("dropoff".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
                            dropoffLocations.add(location);
                        }
                    }

                    adapter.setItems(dropoffLocations);
                    adapter.notifyDataSetChanged();
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

    // Reuse the same LocationAdapter pattern used in SelectPickupPointActivity
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
            Location loc = items.get(position);
            String name = loc.getName();
            String addr = loc.getAddress();
            holder.text.setText((name != null && !name.isEmpty() ? name : (addr != null ? addr : "Không tên")));
            holder.itemView.setActivated(position == selected);
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                int old = selected;
                selected = pos;
                notifyItemChanged(old);
                notifyItemChanged(selected);
                if (onClick != null) onClick.onClick(items.get(pos));
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

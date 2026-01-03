package vn.hcmute.busbooking.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        adapter = new LocationAdapter(this, new ArrayList<>(), loc -> selectedPickup = loc);
        rvLocations.setAdapter(adapter);

        // Display subtotal
        if (seatLabels != null && trip != null) {
            double totalAmount = seatLabels.size() * trip.getPrice();
            tvSubtotal.setText(CurrencyUtil.formatVND(totalAmount));
        }

        // Fetch locations
        fetchPickupLocations();

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

    private static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.VH> {
        interface OnClick { void onClick(Location loc); }
        private final List<Location> items;
        private final OnClick onClick;
        private int selected = -1;
        private final Context context;

        LocationAdapter(Context context, List<Location> items, OnClick onClick) {
            this.context = context;
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_stop, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Location loc = items.get(pos);
            String name = loc.getName();
            String addr = loc.getAddress();
            String time = loc.getEstimatedTime();

            holder.tvName.setText((name != null && !name.isEmpty() ? name : "Không tên"));
            if (addr != null && !addr.isEmpty()) {
                holder.tvAddress.setText(addr);
                holder.tvAddress.setVisibility(View.VISIBLE);
            } else {
                holder.tvAddress.setVisibility(View.GONE);
            }

            if (time != null && !time.isEmpty()) {
                holder.tvStopTime.setText(formatTime(time));
                holder.tvStopTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvStopTime.setVisibility(View.GONE);
            }

            // --- UI State Handling ---
            boolean isSelected = (pos == selected);
            MaterialCardView card = (MaterialCardView) holder.itemView;
            
            holder.rbSelection.setChecked(isSelected);
            holder.viewIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

            if (isSelected) {
                // Selected: Viền xanh, Nền xanh nhạt
                card.setStrokeColor(ContextCompat.getColor(context, R.color.colorPrimary));
                card.setStrokeWidth(4);
                // Use light blue background color (hardcoded or from resources)
                // Using #DFF2FF similar to the image
                card.setCardBackgroundColor(Color.parseColor("#DFF2FF"));
            } else {
                // Normal: Viền xám nhạt, Nền trắng
                card.setStrokeColor(Color.parseColor("#E0E0E0"));
                card.setStrokeWidth(2);
                card.setCardBackgroundColor(Color.WHITE);
            }

            // Click listener for the whole card
            holder.itemView.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                int old = selected;
                selected = p;
                notifyItemChanged(old);
                notifyItemChanged(selected);
                if (onClick != null) onClick.onClick(items.get(p));
            });

            // Map button click (Open in Google Maps)
            holder.llMapAction.setOnClickListener(v -> {
                if (loc.getAddress() != null && !loc.getAddress().isEmpty()) {
                    try {
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(loc.getAddress()));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        context.startActivity(mapIntent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Không thể mở bản đồ", Toast.LENGTH_SHORT).show();
                    }
                } else {
                     Toast.makeText(context, "Không có địa chỉ để hiển thị", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String formatTime(String isoString) {
            if (isoString == null) return "";
            try {
                // Parse ISO string (e.g. 2023-10-27T09:00:00.000)
                // Handle optional Z or offset if present, though formatLocalISO usually strips them
                String s = isoString.replace("Z", "");
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                // Try to parse partial seconds if present
                if (s.length() > 19) {
                     // 2023-10-27T09:00:00.000
                     isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
                }
                
                Date date = isoFormat.parse(s);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return timeFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvAddress;
            TextView tvStopTime;
            RadioButton rbSelection;
            View viewIndicator;
            View llMapAction;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvLocationName);
                tvAddress = itemView.findViewById(R.id.tvLocationAddress);
                tvStopTime = itemView.findViewById(R.id.tvStopTime);
                rbSelection = itemView.findViewById(R.id.rbSelection);
                viewIndicator = itemView.findViewById(R.id.viewSelectionIndicator);
                llMapAction = itemView.findViewById(R.id.llMapAction);
            }
        }
    }
}

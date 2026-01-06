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
import vn.hcmute.busbooking.utils.CurrencyUtil;

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

        // Setup toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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
        adapter = new LocationAdapter(this, new ArrayList<>(), loc -> selectedDropoff = loc);
        rvLocations.setAdapter(adapter);

        // Display subtotal
        if (seatLabels != null && trip != null) {
            double totalAmount = seatLabels.size() * trip.getPrice();
            tvSubtotal.setText(CurrencyUtil.formatVND(totalAmount));
        }

        // Fetch locations
        fetchDropoffLocations();

        // Handle continue button click
        btnContinue.setOnClickListener(v -> {
            if (selectedDropoff == null) {
                Toast.makeText(this, "Vui lòng chọn điểm trả.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent src = getIntent();
                boolean isRoundTrip = src.getBooleanExtra("isRoundTrip", false);
                boolean roundPhase = src.getBooleanExtra("round_trip_phase", false);

                // If this was launched as part of initial round-trip booking (not return-selection phase),
                // and round-trip is requested, then after creating depart selections we should launch TripListActivity
                // for the return trip (so user chooses return trip then its seats/pickup/dropoff). We pass depart_* keys.
                if (isRoundTrip && !roundPhase) {
                    Intent toReturnSearch = new Intent(this, TripListActivity.class);
                    // For return search we swap origin/destination
                    String depOrigin = trip.getOrigin();
                    String depDestination = trip.getDestination();
                    // Use returnOrigin/returnDestination from src if present (TripListActivity expects origin/destination for search)
                    String returnOrigin = src.getStringExtra("returnOrigin");
                    String returnDestination = src.getStringExtra("returnDestination");
                    if (returnOrigin == null || returnOrigin.isEmpty()) returnOrigin = depDestination;
                    if (returnDestination == null || returnDestination.isEmpty()) returnDestination = depOrigin;

                    toReturnSearch.putExtra("origin", returnOrigin);
                    toReturnSearch.putExtra("destination", returnDestination);

                    // Pass returnDate if present (picked earlier)
                    String returnDate = src.getStringExtra("returnDate");
                    if (returnDate != null) toReturnSearch.putExtra("date", returnDate);

                    // Mark that TripList should operate as return-selection and carry depart_* details
                    toReturnSearch.putExtra("round_trip_phase", true);

                    // embed depart selections so return flow can assemble final booking
                    toReturnSearch.putExtra("depart_trip", trip);
                    toReturnSearch.putStringArrayListExtra("depart_seat_labels", seatLabels == null ? new java.util.ArrayList<>() : seatLabels);
                    // Explicitly cast parcelable extras to concrete types so putExtra overload is unambiguous
                    toReturnSearch.putExtra("depart_pickup_location", (Location) src.getParcelableExtra("pickup_location"));
                    toReturnSearch.putExtra("depart_dropoff_location", selectedDropoff);
                    toReturnSearch.putExtra("isRoundTrip", true);

                    startActivity(toReturnSearch);
                    return;
                }

                // Otherwise (not round-trip or this is the return-selection phase), proceed to ContactInfoActivity
                Intent intent = new Intent(this, ContactInfoActivity.class);
                intent.putExtra("trip", trip);
                intent.putStringArrayListExtra("seat_labels", seatLabels);
                intent.putExtra("pickup_location", (Location) src.getParcelableExtra("pickup_location"));
                intent.putExtra("dropoff_location", selectedDropoff);

                // If this is part of a return-selection phase, also forward the depart_* extras so ContactInfoActivity can show both legs
                if (roundPhase) {
                    intent.putExtra("return_trip", trip);
                    intent.putStringArrayListExtra("return_seat_labels", seatLabels);
                    intent.putExtra("return_pickup_location", (Location) src.getParcelableExtra("pickup_location"));
                    intent.putExtra("return_dropoff_location", selectedDropoff);

                    // forward depart_* that were stored earlier
                    Trip departTrip = (Trip) src.getParcelableExtra("depart_trip");
                    java.util.ArrayList<String> departSeats = src.getStringArrayListExtra("depart_seat_labels");
                    Location departPickup = (Location) src.getParcelableExtra("depart_pickup_location");
                    Location departDropoff = (Location) src.getParcelableExtra("depart_dropoff_location");
                    if (departTrip != null) intent.putExtra("trip", departTrip); // set primary trip as depart
                    if (departSeats != null) intent.putStringArrayListExtra("seat_labels", departSeats);
                    if (departPickup != null) intent.putExtra("pickup_location", departPickup);
                    if (departDropoff != null) intent.putExtra("dropoff_location", departDropoff);

                    intent.putExtra("isRoundTrip", true);
                } else {
                    // single leg or normal flow
                }

                startActivity(intent);
            } catch (Exception e) {
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
            int pos = holder.getBindingAdapterPosition();
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
                card.setCardBackgroundColor(Color.parseColor("#DFF2FF"));
            } else {
                // Normal: Viền xám nhạt, Nền trắng
                card.setStrokeColor(Color.parseColor("#E0E0E0"));
                card.setStrokeWidth(2);
                card.setCardBackgroundColor(Color.WHITE);
            }

            holder.itemView.setOnClickListener(v -> {
                int p = holder.getBindingAdapterPosition();
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
                String s = isoString.replace("Z", "");
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                if (s.length() > 19) {
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

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.SeatAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.CurrencyUtil;

public class SeatSelectionActivity extends AppCompatActivity {

    private RecyclerView floor1RecyclerView, floor2RecyclerView;
    private TextView tvSubtotal;
    private Button btnContinue;
    private Trip trip;
    private SeatAdapter floor1Adapter, floor2Adapter;
    private final List<Seat> floor1Seats = new ArrayList<>();
    private final List<Seat> floor2Seats = new ArrayList<>();
    private final Set<String> selectedSeats = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        trip = getIntent().getParcelableExtra("trip");
        if (trip == null || trip.getSeatLayout() == null || trip.getSeatLayout().isEmpty()) {
            Toast.makeText(this, "Sơ đồ ghế không có sẵn", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        fetchSeatStatuses();
    }

    private void initViews() {
        floor1RecyclerView = findViewById(R.id.floor1RecyclerView);
        floor2RecyclerView = findViewById(R.id.floor2RecyclerView);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ghế", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, SelectPickupPointActivity.class);
            intent.putExtra("trip", trip);
            intent.putStringArrayListExtra("seat_labels", new ArrayList<>(selectedSeats));
            // Forward round-trip related extras if present so next screens know about return flow
            try {
                Intent src = getIntent();
                // Prefer isRoundTrip but accept legacy isReturn for compatibility
                if (src.getBooleanExtra("isRoundTrip", src.getBooleanExtra("isReturn", false))) intent.putExtra("isRoundTrip", true);
                String returnDate = src.getStringExtra("returnDate"); if (returnDate != null) intent.putExtra("returnDate", returnDate);
                String returnOrigin = src.getStringExtra("returnOrigin"); if (returnOrigin != null) intent.putExtra("returnOrigin", returnOrigin);
                String returnDestination = src.getStringExtra("returnDestination"); if (returnDestination != null) intent.putExtra("returnDestination", returnDestination);
                boolean isRoundTrip = src.getBooleanExtra("isRoundTrip", false); if (isRoundTrip) intent.putExtra("isRoundTrip", true);

                // Normalize round_trip_phase: accept boolean or string from caller
                boolean roundPhase = src.getBooleanExtra("round_trip_phase", false);
                if (!roundPhase) {
                    String phaseStr = src.getStringExtra("round_trip_phase");
                    roundPhase = phaseStr != null && (phaseStr.equalsIgnoreCase("true") || phaseStr.equals("1"));
                }
                if (roundPhase) intent.putExtra("round_trip_phase", true);

                // Only forward depart_* keys when we're selecting return trip seats (roundPhase == true)
                if (roundPhase) {
                    // We're selecting return trip seats, forward depart_* keys
                    android.os.Parcelable departTrip = src.getParcelableExtra("depart_trip");
                    java.util.ArrayList<String> departSeats = src.getStringArrayListExtra("depart_seat_labels");
                    android.os.Parcelable departPickup = src.getParcelableExtra("depart_pickup_location");
                    android.os.Parcelable departDropoff = src.getParcelableExtra("depart_dropoff_location");
                    if (departTrip != null) intent.putExtra("depart_trip", departTrip);
                    if (departSeats != null) intent.putStringArrayListExtra("depart_seat_labels", departSeats);
                    if (departPickup != null) intent.putExtra("depart_pickup_location", departPickup);
                    if (departDropoff != null) intent.putExtra("depart_dropoff_location", departDropoff);
                    intent.putExtra("round_trip_phase", true);
                }
            } catch (Exception ignored) {}
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void fetchSeatStatuses() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        // Revert to fetching all seats and checking their status on the client-side.
        ApiClient.getClient().create(ApiService.class).getSeats(trip.getId(), null).enqueue(new Callback<List<Seat>>() {
            @Override
            public void onResponse(Call<List<Seat>> call, Response<List<Seat>> response) {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> bookedSeats = new HashSet<>();
                    // Iterate through all seats returned by the API and check their `isBooked` status.
                    for (Seat seat : response.body()) {
                        if (seat.isBooked() && seat.getLabel() != null) {
                            // Normalize label (trim + uppercase) to avoid format mismatches
                            bookedSeats.add(seat.getLabel().trim().toUpperCase());
                        }
                    }
                    generateSeatMap(bookedSeats);
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Không thể tải trạng thái ghế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Seat>> call, Throwable t) {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateSeatMap(Set<String> bookedSeats) {
        floor1Seats.clear();
        floor2Seats.clear();

        Gson gson = new Gson();
        SeatLayout layout = gson.fromJson(trip.getSeatLayout(), SeatLayout.class);

        if (layout == null || layout.floors == null || layout.floors.isEmpty()) {
            Toast.makeText(this, "Lỗi đọc sơ đồ ghế", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tầng 1 (Tầng dưới)
        if (layout.floors.size() > 0) {
            Floor floor1 = layout.floors.get(0);
            populateFloor(floor1, floor1Seats, bookedSeats, 0);
            floor1Adapter = new SeatAdapter(floor1Seats, this::onSeatSelected);
            // For 32-seat bus (1 extra end bed), keep 3 columns. Otherwise use extra_end_bed count.
            int floor1Cols = (floor1.extra_end_bed > 0 && floor1.extra_end_bed != 1) ? floor1.extra_end_bed : floor1.cols;
            floor1RecyclerView.setLayoutManager(new GridLayoutManager(this, floor1Cols));
            floor1RecyclerView.setAdapter(floor1Adapter);
        }

        // Tầng 2 (Tầng trên)
        if (layout.floors.size() > 1) {
            Floor floor2 = layout.floors.get(1);
            populateFloor(floor2, floor2Seats, bookedSeats, 1);
            floor2Adapter = new SeatAdapter(floor2Seats, this::onSeatSelected);
            // For 32-seat bus (1 extra end bed), keep 3 columns. Otherwise use extra_end_bed count.
            int floor2Cols = (floor2.extra_end_bed > 0 && floor2.extra_end_bed != 1) ? floor2.extra_end_bed : floor2.cols;
            floor2RecyclerView.setLayoutManager(new GridLayoutManager(this, floor2Cols));
            floor2RecyclerView.setAdapter(floor2Adapter);
            findViewById(R.id.tvFloor2Header).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tvFloor2Header).setVisibility(View.GONE);
        }

        updateSelectedInfo();
    }

    private void populateFloor(Floor floor, List<Seat> seatList, Set<String> bookedSeats, int floorIndex) {
        if (floor.seats == null) return;

        // Calculate total rows including extra end bed row
        int actualRows = floor.rows;
        int actualCols = floor.cols;
        boolean hasExtraEndBed = floor.extra_end_bed > 0;

        // If there are extra end beds, add an extra row
        if (hasExtraEndBed) {
            actualRows = floor.rows + 1;
            // The grid width depends on the number of extra end beds
            // For 1 extra end bed (32-seat bus), keep 3 columns to show all regular seats
            // For 4-5 extra end beds, use extra_end_bed count for uniform layout
            if (floor.extra_end_bed == 1 && floor.cols == 3) {
                actualCols = 3; // Keep 3 columns for 32-seat bus
            } else {
                actualCols = floor.extra_end_bed;
            }
        }

        int totalCells = actualRows * actualCols;
        Seat[] seatGrid = new Seat[totalCells];

        // Initialize all cells as aisle (empty)
        for (int i = 0; i < totalCells; i++) {
            Seat aisle = new Seat("");
            aisle.setSeatType("aisle");
            seatGrid[i] = aisle;
        }

        // Sort seat infos by row then col so numbering is consistent
        List<SeatInfo> seatInfos = new ArrayList<>(floor.seats);
        Collections.sort(seatInfos, (a, b) -> {
            if (a.row != b.row) return Integer.compare(a.row, b.row);
            return Integer.compare(a.col, b.col);
        });

        String seatLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String prefix = floorIndex >= 0 && floorIndex < seatLetters.length() ? String.valueOf(seatLetters.charAt(floorIndex)) : "A";
        int seq = 1;
        for (SeatInfo seatInfo : seatInfos) {
            int gridCol = seatInfo.col;

            // Apply column mapping for regular rows with 3 columns to create aisles
            if (hasExtraEndBed && seatInfo.row < floor.rows && floor.cols == 3) {
                if (floor.extra_end_bed == 5) {
                    // Xe 40 chỗ (5 ghế băng cuối): Map col 0 -> 0, col 1 -> 2, col 2 -> 4 (lối đi ở cột 1, 3)
                    gridCol = seatInfo.col * 2;
                } else if (floor.extra_end_bed == 4) {
                    // Xe 38 chỗ (4 ghế băng cuối): Map col 0 -> 0, col 1 -> 2, col 2 -> 3 (lối đi ở cột 1)
                    if (seatInfo.col == 0) {
                        gridCol = 0;
                    } else if (seatInfo.col == 1) {
                        gridCol = 2;
                    } else if (seatInfo.col == 2) {
                        gridCol = 3;
                    }
                }
                // For 1 extra end bed (32-seat bus), no mapping needed - keep original 3-column layout
            }
            
            // For the extra end bed row seats (already at correct position - col 0)
            // No need to adjust position for 32-seat bus extra end bed

            int index = seatInfo.row * actualCols + gridCol;
            if (index >= 0 && index < totalCells) {
                String displayLabel = prefix + seq;
                seq++;
                Seat seat = new Seat(displayLabel);
                seat.setSeatType(seatInfo.type);
                // Mark booked if API returned booking using either original label or the display label
                // Normalize seat labels before checking against booked set
                String seatInfoLabelNorm = seatInfo.label != null ? seatInfo.label.trim().toUpperCase() : null;
                String displayLabelNorm = displayLabel != null ? displayLabel.trim().toUpperCase() : null;
                if ((seatInfoLabelNorm != null && bookedSeats.contains(seatInfoLabelNorm)) || (displayLabelNorm != null && bookedSeats.contains(displayLabelNorm))) {
                    seat.setBooked(true);
                }
                seatGrid[index] = seat;
            }
        }

        Collections.addAll(seatList, seatGrid);
    }

    private void onSeatSelected(Seat seat) {
        if (seat.isBooked() || seat.getLabel() == null || seat.getLabel().isEmpty() || "aisle".equals(seat.getSeatType())) {
            return;
        }

        if (selectedSeats.contains(seat.getLabel())) {
            selectedSeats.remove(seat.getLabel());
        } else {
            selectedSeats.add(seat.getLabel());
        }
        seat.setSelected(!seat.isSelected());

        int indexInFloor1 = floor1Seats.indexOf(seat);
        if (indexInFloor1 != -1 && floor1Adapter != null) {
            floor1Adapter.notifyItemChanged(indexInFloor1);
        } else {
            int indexInFloor2 = floor2Seats.indexOf(seat);
            if (indexInFloor2 != -1 && floor2Adapter != null) {
                floor2Adapter.notifyItemChanged(indexInFloor2);
            }
        }

        updateSelectedInfo();
    }

    private void updateSelectedInfo() {
        double totalAmount = selectedSeats.size() * trip.getPrice();
        tvSubtotal.setText(CurrencyUtil.formatVND(totalAmount));
        btnContinue.setEnabled(!selectedSeats.isEmpty());
    }

    static class SeatLayout {
        List<Floor> floors;
    }

    static class Floor {
        int floor;
        int rows;
        int cols;
        int extra_end_bed;
        List<SeatInfo> seats;
    }

    static class SeatInfo {
        String label;
        String type;
        int row;
        int col;
    }
}

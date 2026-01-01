package vn.hcmute.busbooking.activity.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.SeatAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.utils.SessionManager;

public class AdminAddBookingActivity extends AppCompatActivity {

    private RecyclerView floor1RecyclerView, floor2RecyclerView;
    private TextView tvTripInfo, tvTotalPrice, tvSeatCount;
    private Button btnCreateBooking;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private SeatAdapter floor1Adapter, floor2Adapter;
    private final List<Seat> floor1Seats = new ArrayList<>();
    private final List<Seat> floor2Seats = new ArrayList<>();
    private final Set<String> selectedSeats = new HashSet<>();

    private int tripId;
    private double tripPrice = 0;
    private String tripInfo = "";
    private int floor1Cols = 4; // Default columns, will be updated from layout
    private int floor2Cols = 4;

    // ...existing code...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_booking);

        // Get trip ID from intent
        tripId = getIntent().getIntExtra("trip_id", -1);
        if (tripId == -1) {
            Toast.makeText(this, "Invalid trip", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupToolbar();
        fetchTripDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        floor1RecyclerView = findViewById(R.id.floor1RecyclerView);
        floor2RecyclerView = findViewById(R.id.floor2RecyclerView);
        tvTripInfo = findViewById(R.id.tvTripInfo);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvSeatCount = findViewById(R.id.tvSeatCount);
        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        progressBar = findViewById(R.id.progressBar);

        btnCreateBooking.setOnClickListener(v -> validateAndCreateBooking());

        // Initialize adapters with empty lists to prevent NullPointerException
        floor1Adapter = new SeatAdapter(floor1Seats, seat -> onSeatSelected(seat, 1));
        floor2Adapter = new SeatAdapter(floor2Seats, seat -> onSeatSelected(seat, 2));

        GridLayoutManager layoutManager1 = new GridLayoutManager(this, floor1Cols);
        floor1RecyclerView.setLayoutManager(layoutManager1);
        floor1RecyclerView.setAdapter(floor1Adapter);

        GridLayoutManager layoutManager2 = new GridLayoutManager(this, floor2Cols);
        floor2RecyclerView.setLayoutManager(layoutManager2);
        floor2RecyclerView.setAdapter(floor2Adapter);
    }

    private void onSeatSelected(Seat seat, int floorNum) {
        if (!seat.isBooked()) {
            seat.setSelected(!seat.isSelected());
            if (seat.isSelected()) {
                selectedSeats.add(seat.getLabel());
            } else {
                selectedSeats.remove(seat.getLabel());
            }
            floor1Adapter.notifyDataSetChanged();
            floor2Adapter.notifyDataSetChanged();
            updateSeatCount();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSeatAdapters() {
        // Update column counts and layout managers based on parsed layout
        GridLayoutManager layoutManager1 = new GridLayoutManager(this, floor1Cols);
        floor1RecyclerView.setLayoutManager(layoutManager1);

        GridLayoutManager layoutManager2 = new GridLayoutManager(this, floor2Cols);
        floor2RecyclerView.setLayoutManager(layoutManager2);

        // Notify adapters that data has changed
        floor1Adapter.notifyDataSetChanged();
        floor2Adapter.notifyDataSetChanged();
    }

    private void fetchTripDetails() {
        progressBar.setVisibility(View.VISIBLE);
        int userId = sessionManager.getUserId();
        apiService.getAdminTripDetails(userId, tripId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> trip = response.body();
                    tripPrice = parseDouble(trip.get("price"));
                    tripInfo = String.format("%s - %s", trip.get("origin"), trip.get("destination"));
                    tvTripInfo.setText(tripInfo);

                    // Lấy seat_layout từ trip details (lấy từ buses table)
                    Object seatLayoutObj = trip.get("seat_layout");
                    if (seatLayoutObj != null) {
                        parseSeatLayout(seatLayoutObj);
                    } else {
                        android.util.Log.w("AdminAddBooking", "No seat_layout in trip details");
                        floor1Seats.clear();
                        floor2Seats.clear();
                        setupSeatAdapters();
                        Toast.makeText(AdminAddBookingActivity.this, "Chuyến này chưa được cấu hình ghế", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminAddBookingActivity.this, "Failed to load trip details", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAddBookingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // Xử lý seat_layout từ response
    private void parseSeatLayout(Object seatLayoutObj) {
        try {
            // Xử lý Gson JsonObject trả về từ API
            if (seatLayoutObj == null) {
                Toast.makeText(this, "Sơ đồ ghế trống", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> layout = null;

            // Convert Gson JsonElement to Map if needed
            if (seatLayoutObj instanceof Map) {
                layout = (Map<String, Object>) seatLayoutObj;
            } else {
                // Try to convert JsonObject to Map using Gson
                com.google.gson.Gson gson = new com.google.gson.Gson();
                layout = gson.fromJson(gson.toJson(seatLayoutObj), Map.class);
            }

            if (layout == null || !layout.containsKey("floors")) {
                Toast.makeText(this, "Cấu trúc sơ đồ ghế không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Map<String, Object>> floors = (List<Map<String, Object>>) layout.get("floors");
            if (floors != null && floors.size() > 0) {
                // Parse floor 1 (Tầng A)
                Map<String, Object> floor1 = floors.get(0);
                double colsObj = 0;
                if (floor1.containsKey("cols")) {
                    Object colVal = floor1.get("cols");
                    if (colVal instanceof Number) {
                        colsObj = ((Number) colVal).doubleValue();
                    }
                }
                floor1Cols = (int) colsObj;
                if (floor1Cols <= 0) floor1Cols = 3; // Default to 3 if invalid

                // Parse floor 2 (Tầng B) if exists
                if (floors.size() > 1) {
                    Map<String, Object> floor2 = floors.get(1);
                    double colsObj2 = 0;
                    if (floor2.containsKey("cols")) {
                        Object colVal = floor2.get("cols");
                        if (colVal instanceof Number) {
                            colsObj2 = ((Number) colVal).doubleValue();
                        }
                    }
                    floor2Cols = (int) colsObj2;
                    if (floor2Cols <= 0) floor2Cols = 3; // Default to 3 if invalid
                }
            }

            generateSeatMap(floors);
        } catch (Exception e) {
            android.util.Log.e("AdminAddBooking", "Error parsing seat layout", e);
            Toast.makeText(this, "Lỗi xử lý cấu trúc ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generateSeatMap(List<Map<String, Object>> floors) {
        floor1Seats.clear();
        floor2Seats.clear();

        if (floors == null || floors.size() == 0) {
            android.util.Log.d("AdminAddBooking", "No floors data");
            return;
        }

        // Process each floor
        for (int floorIdx = 0; floorIdx < Math.min(floors.size(), 2); floorIdx++) {
            Map<String, Object> floor = floors.get(floorIdx);
            List<Map<String, Object>> seatsList = (List<Map<String, Object>>) floor.get("seats");

            if (seatsList == null) {
                android.util.Log.d("AdminAddBooking", "Floor " + floorIdx + " has no seats");
                continue;
            }

            // Create Seat objects from layout
            List<Seat> floorSeats = new ArrayList<>();
            for (Map<String, Object> seatMap : seatsList) {
                String label = (String) seatMap.get("label");
                String seatType = (String) seatMap.get("type");
                Object bookedObj = seatMap.get("isBooked");
                boolean isBooked = false;
                if (bookedObj instanceof Boolean) {
                    isBooked = (Boolean) bookedObj;
                }

                // Skip aisle seats - they shouldn't be displayed
                if ("aisle".equals(seatType)) {
                    continue;
                }

                Seat seat = new Seat();
                seat.setLabel(label);
                seat.setSeatType(seatType != null ? seatType : "bed");
                seat.setBooked(isBooked);

                // Store row/col for sorting
                Object rowObj = seatMap.get("row");
                Object colObj = seatMap.get("col");
                int row = rowObj instanceof Number ? ((Number) rowObj).intValue() : 0;
                int col = colObj instanceof Number ? ((Number) colObj).intValue() : 0;

                floorSeats.add(seat);
            }

            // Sort seats by row, then by col to maintain grid layout
            floorSeats.sort((s1, s2) -> {
                // Extract row and col from label or order
                // Since label is like A1, A2, B1, we need to parse it
                // For now, maintain insertion order but could enhance this
                return 0; // Labels should already be in correct order from backend
            });

            if (floorIdx == 0) {
                floor1Seats.addAll(floorSeats);
            } else if (floorIdx == 1) {
                floor2Seats.addAll(floorSeats);
            }
        }

        // Re-setup adapters with correct column counts
        setupSeatAdapters();

        android.util.Log.d("AdminAddBooking", "Floor A: " + floor1Seats.size() + " seats (" + floor1Cols + " cols), Floor B: " + floor2Seats.size() + " seats (" + floor2Cols + " cols)");
    }

    private void updateSeatCount() {
        int count = selectedSeats.size();
        tvSeatCount.setText(String.format(Locale.getDefault(), "%d ghế", count));
        tvTotalPrice.setText("(Đánh dấu)");
    }

    private void validateAndCreateBooking() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
            return;
        }

        // Since we're just marking seats as sold, no passenger info needed
        createBooking();
    }

    private void createBooking() {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> body = new HashMap<>();
        body.put("trip_id", tripId);
        body.put("seat_labels", new ArrayList<>(selectedSeats));

        int userId = sessionManager.getUserId();
        apiService.adminCreateBooking(userId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminAddBookingActivity.this, "Đánh dấu ghế thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminAddBookingActivity.this, "Lỗi: Không thể đánh dấu ghế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAddBookingActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDouble(Object obj) {
        if (obj == null) return 0;
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

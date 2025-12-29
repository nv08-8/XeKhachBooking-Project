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
        setupSeatAdapters();
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSeatAdapters() {
        // Floor 1 (Tầng A) Adapter
        floor1Adapter = new SeatAdapter(floor1Seats, seat -> {
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
        });

        GridLayoutManager layoutManager1 = new GridLayoutManager(this, 4);
        floor1RecyclerView.setLayoutManager(layoutManager1);
        floor1RecyclerView.setAdapter(floor1Adapter);

        // Floor 2 (Tầng B) Adapter
        floor2Adapter = new SeatAdapter(floor2Seats, seat -> {
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
        });

        GridLayoutManager layoutManager2 = new GridLayoutManager(this, 4);
        floor2RecyclerView.setLayoutManager(layoutManager2);
        floor2RecyclerView.setAdapter(floor2Adapter);
    }

    private void fetchTripDetails() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getTripDetails(tripId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> trip = response.body();
                    tripPrice = parseDouble(trip.get("price"));
                    tripInfo = String.format("%s - %s", trip.get("origin"), trip.get("destination"));
                    tvTripInfo.setText(tripInfo);
                    fetchSeats();
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

    private void fetchSeats() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getSeats(tripId).enqueue(new Callback<List<Seat>>() {
            @Override
            public void onResponse(Call<List<Seat>> call, Response<List<Seat>> response) {
                progressBar.setVisibility(View.GONE);

                android.util.Log.d("AdminAddBooking", "fetchSeats response code: " + response.code());
                android.util.Log.d("AdminAddBooking", "fetchSeats response successful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    // Nếu API trả về danh sách ghế (có thể rỗng hoặc có dữ liệu)
                    android.util.Log.d("AdminAddBooking", "Got " + response.body().size() + " seats from API");
                    generateSeatMap(response.body());

                    if (response.body().isEmpty()) {
                        Toast.makeText(AdminAddBookingActivity.this, "Chuyến này chưa có ghế trong hệ thống", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminAddBookingActivity.this, "Danh sách ghế đã tải thành công", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Nếu API call fail (error status code)
                    android.util.Log.d("AdminAddBooking", "API call failed with status: " + response.code());
                    floor1Seats.clear();
                    floor2Seats.clear();
                    floor1Adapter.notifyDataSetChanged();
                    floor2Adapter.notifyDataSetChanged();
                    Toast.makeText(AdminAddBookingActivity.this, "Không thể tải danh sách ghế từ server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Seat>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                android.util.Log.e("AdminAddBooking", "fetchSeats failed: " + t.getMessage(), t);
                // Nếu request fail hoàn toàn
                floor1Seats.clear();
                floor2Seats.clear();
                floor1Adapter.notifyDataSetChanged();
                floor2Adapter.notifyDataSetChanged();
                Toast.makeText(AdminAddBookingActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateSeatMap(List<Seat> allSeats) {
        floor1Seats.clear();
        floor2Seats.clear();

        for (Seat seat : allSeats) {
            if (seat.getLabel() != null) {
                char firstChar = seat.getLabel().charAt(0);
                // Tầng A (starts with A)
                if (firstChar == 'A' || firstChar == 'a') {
                    floor1Seats.add(seat);
                }
                // Tầng B (starts with B)
                else if (firstChar == 'B' || firstChar == 'b') {
                    floor2Seats.add(seat);
                }
            }
        }

        floor1Adapter.notifyDataSetChanged();
        floor2Adapter.notifyDataSetChanged();

        android.util.Log.d("AdminAddBooking", "Floor A: " + floor1Seats.size() + " seats, Floor B: " + floor2Seats.size() + " seats");
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

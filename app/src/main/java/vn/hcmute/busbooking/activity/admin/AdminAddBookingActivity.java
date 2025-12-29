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

    private RecyclerView seatRecyclerView;
    private TextView tvTripInfo, tvTotalPrice, tvSeatCount;
    private Button btnCreateBooking;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private SeatAdapter seatAdapter;
    private final List<Seat> seatList = new ArrayList<>();
    private final Set<String> selectedSeats = new HashSet<>();

    private int tripId;
    private double tripPrice = 0;
    private String tripInfo = "";
    private int totalSeats = 0; // Biến để lưu tổng số ghế

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
        setupSeatAdapter();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        seatRecyclerView = findViewById(R.id.seatRecyclerView);
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

    private void setupSeatAdapter() {
        seatAdapter = new SeatAdapter(seatList, seat -> {
            if (!seat.isBooked()) {
                seat.setSelected(!seat.isSelected());
                if (seat.isSelected()) {
                    selectedSeats.add(seat.getLabel());
                } else {
                    selectedSeats.remove(seat.getLabel());
                }
                seatAdapter.notifyDataSetChanged();
                updateSeatCount();
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        seatRecyclerView.setLayoutManager(layoutManager);
        seatRecyclerView.setAdapter(seatAdapter);
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

                    // Lấy seats_total từ trip details
                    Object seatsObj = trip.get("seats_total");
                    if (seatsObj instanceof Number) {
                        totalSeats = ((Number) seatsObj).intValue();
                    } else if (seatsObj instanceof String) {
                        try {
                            totalSeats = Integer.parseInt(seatsObj.toString());
                        } catch (NumberFormatException e) {
                            totalSeats = 40; // Mặc định
                        }
                    }

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

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Nếu API trả về danh sách ghế, sử dụng nó
                    android.util.Log.d("AdminAddBooking", "Got " + response.body().size() + " seats from API");
                    seatList.clear();
                    seatList.addAll(response.body());
                    seatAdapter.notifyDataSetChanged();
                    Toast.makeText(AdminAddBookingActivity.this, "Danh sách ghế đã tải thành công", Toast.LENGTH_SHORT).show();
                } else {
                    // Nếu API trả về danh sách rỗng, tạo danh sách ghế mặc định từ seats_total
                    android.util.Log.d("AdminAddBooking", "API returned empty list or null, generating default seats");
                    android.util.Log.d("AdminAddBooking", "totalSeats: " + totalSeats);
                    if (response.body() != null) {
                        android.util.Log.d("AdminAddBooking", "Response body size: " + response.body().size());
                    }
                    generateDefaultSeats();
                }
            }

            @Override
            public void onFailure(Call<List<Seat>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                android.util.Log.e("AdminAddBooking", "fetchSeats failed: " + t.getMessage(), t);
                // Nếu có lỗi, vẫn thử tạo danh sách ghế mặc định
                generateDefaultSeats();
                Toast.makeText(AdminAddBookingActivity.this, "Không thể tải từ server, sử dụng ghế mặc định", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateDefaultSeats() {
        android.util.Log.d("AdminAddBooking", "generateDefaultSeats() called");
        android.util.Log.d("AdminAddBooking", "Current seatList size: " + seatList.size());
        android.util.Log.d("AdminAddBooking", "totalSeats: " + totalSeats);

        // Luôn tạo danh sách ghế nếu seatList rỗng
        if (seatList.isEmpty()) {
            // Nếu totalSeats không hợp lệ, dùng giá trị mặc định
            int seatsToCreate = (totalSeats > 0) ? totalSeats : 40;
            android.util.Log.d("AdminAddBooking", "Creating " + seatsToCreate + " seats");

            for (int i = 1; i <= seatsToCreate; i++) {
                seatList.add(new Seat("A" + i));
            }
            android.util.Log.d("AdminAddBooking", "Generated " + seatList.size() + " seats");
            seatAdapter.notifyDataSetChanged();
        }
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

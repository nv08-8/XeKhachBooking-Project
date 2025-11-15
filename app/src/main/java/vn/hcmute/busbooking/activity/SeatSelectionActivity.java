package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Build; // Added for API level check
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
import vn.hcmute.busbooking.adapter.SeatSelectionAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class SeatSelectionActivity extends AppCompatActivity {

    private RecyclerView seatRecyclerView;
    private TextView tvTripDetails, tvSelectedSeatsInfo;
    private Button btnConfirmSeat;
    private ImageButton backButton;
    private ProgressBar progressBar;

    private SeatSelectionAdapter seatAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    private Trip trip;
    private final List<Seat> seatList = new ArrayList<>();
    private final Set<String> selectedSeats = new HashSet<>();
    private int seatPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        // Initialize views
        seatRecyclerView = findViewById(R.id.seatRecyclerView);
        tvTripDetails = findViewById(R.id.tvTripDetails);
        tvSelectedSeatsInfo = findViewById(R.id.tvSelectedSeatsInfo);
        btnConfirmSeat = findViewById(R.id.btnConfirmSeat);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        // Get trip from intent (Parcelable-safe)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            trip = getIntent().getParcelableExtra("trip", Trip.class);
        } else {
            trip = getIntent().getParcelableExtra("trip");
        }

        if (trip == null) {
            Toast.makeText(this, "Không tìm thấy thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        seatPrice = (int) trip.getPrice();

        // Display trip details
        String tripInfo = trip.getOrigin() + " → " + trip.getDestination() + "\n" +
                         trip.getDepartureTime() + " - " + trip.getArrivalTime() + "\n" +
                         "Nhà xe: " + trip.getOperator();
        tvTripDetails.setText(tripInfo);

        // Setup RecyclerView
        seatRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        seatAdapter = new SeatSelectionAdapter(seatList, this::onSeatSelected);
        seatRecyclerView.setAdapter(seatAdapter);

        backButton.setOnClickListener(v -> finish());
        btnConfirmSeat.setOnClickListener(v -> confirmBooking());

        loadSeats();
    }

    private void loadSeats() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getSeats(trip.getId(), "true").enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    seatList.clear();

                    for (Map<String, Object> seatData : response.body()) {
                        Seat seat = new Seat();
                        seat.setLabel((String) seatData.get("label"));
                        seat.setAvailable(true); // Only available seats from API
                        seatList.add(seat);
                    }

                    seatAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Không thể tải danh sách ghế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSeatSelected(Seat seat) {
        if (selectedSeats.contains(seat.getLabel())) {
            selectedSeats.remove(seat.getLabel());
        } else {
            selectedSeats.add(seat.getLabel());
        }
        updateSelectedInfo();
    }

    private void updateSelectedInfo() {
        int totalAmount = selectedSeats.size() * seatPrice;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        String info = "Đã chọn: " + selectedSeats.size() + " ghế\n" +
                     "Tổng tiền: " + formatter.format(totalAmount);
        tvSelectedSeatsInfo.setText(info);

        btnConfirmSeat.setEnabled(!selectedSeats.isEmpty());
    }

    private void confirmBooking() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt vé", Toast.LENGTH_SHORT).show();
            // Redirect to login
            Intent intent = new Intent(this, vn.hcmute.busbooking.activity.LoginActivity.class);
            startActivity(intent);
            return;
        }

        // Book the first selected seat (can be extended to book multiple seats)
        String seatLabel = selectedSeats.iterator().next();

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmSeat.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("trip_id", trip.getId());
        body.put("seat_label", seatLabel);

        apiService.createBooking(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnConfirmSeat.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Object bookingIdObj = result.get("booking_id");

                    int bookingId = 0;
                    if (bookingIdObj instanceof Double) {
                        bookingId = ((Double) bookingIdObj).intValue();
                    } else if (bookingIdObj instanceof Integer) {
                        bookingId = (Integer) bookingIdObj;
                    }

                    Toast.makeText(SeatSelectionActivity.this, "Đặt vé thành công!", Toast.LENGTH_SHORT).show();

                    // Navigate to payment
                    Intent intent = new Intent(SeatSelectionActivity.this, PaymentActivity.class);
                    intent.putExtra("booking_id", bookingId);
                    intent.putExtra("origin", trip.getOrigin());
                    intent.putExtra("destination", trip.getDestination());
                    intent.putExtra("seat_label", seatLabel);
                    intent.putExtra("amount", seatPrice);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Đặt vé thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirmSeat.setEnabled(true);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

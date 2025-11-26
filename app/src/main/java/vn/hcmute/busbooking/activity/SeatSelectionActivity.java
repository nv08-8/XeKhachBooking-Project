package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void fetchSeatStatuses() {
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        ApiClient.getClient().create(ApiService.class).getSeats(trip.getId(), null).enqueue(new Callback<List<Seat>>() {
            @Override
            public void onResponse(Call<List<Seat>> call, Response<List<Seat>> response) {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> bookedSeats = new HashSet<>();
                    for (Seat seat : response.body()) {
                        if (seat.isBooked()) {
                            bookedSeats.add(seat.getLabel());
                        }
                    }
                    generateSeatMap(bookedSeats);
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Không thể tải trạng thái ghế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Seat>> call, Throwable t) {
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.GONE);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
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
            populateFloor(floor1, floor1Seats, bookedSeats);
            floor1Adapter = new SeatAdapter(floor1Seats, this::onSeatSelected);
            floor1RecyclerView.setLayoutManager(new GridLayoutManager(this, floor1.cols));
            floor1RecyclerView.setAdapter(floor1Adapter);
        }

        // Tầng 2 (Tầng trên)
        if (layout.floors.size() > 1) {
            Floor floor2 = layout.floors.get(1);
            populateFloor(floor2, floor2Seats, bookedSeats);
            floor2Adapter = new SeatAdapter(floor2Seats, this::onSeatSelected);
            floor2RecyclerView.setLayoutManager(new GridLayoutManager(this, floor2.cols));
            floor2RecyclerView.setAdapter(floor2Adapter);
        } else {
            findViewById(R.id.tvFloor2Header).setVisibility(View.GONE);
        }

        updateSelectedInfo();
    }

    private void populateFloor(Floor floor, List<Seat> seatList, Set<String> bookedSeats) {
        char[] seatLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        for (int r = 0; r < floor.rows; r++) {
            for (int c = 0; c < floor.cols; c++) {
                String seatLabel;
                String seatType = "bed";

                if (floor.cols == 3 && c == 1) {
                    seatType = "aisle";
                    seatLabel = "";
                } else {
                    int seatIndex = c;
                    if (floor.cols == 3 && c > 1) {
                        seatIndex = c - 1;
                    }
                    seatLabel = seatLetters[seatIndex] + String.valueOf(r + 1);
                }

                Seat seat = new Seat(seatLabel);
                seat.setSeatType(seatType);
                if (bookedSeats.contains(seatLabel)) {
                    seat.setBooked(true);
                }
                seatList.add(seat);
            }
        }
    }

    private void onSeatSelected(Seat seat) {
        if (selectedSeats.contains(seat.getLabel())) {
            selectedSeats.remove(seat.getLabel());
        } else {
            selectedSeats.add(seat.getLabel());
        }
        seat.setSelected(!seat.isSelected());

        // Cập nhật lại cả 2 adapter để giao diện được đồng bộ
        if (floor1Adapter != null) floor1Adapter.notifyDataSetChanged();
        if (floor2Adapter != null) floor2Adapter.notifyDataSetChanged();

        updateSelectedInfo();
    }

    private void updateSelectedInfo() {
        double totalAmount = selectedSeats.size() * trip.getPrice();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSubtotal.setText(formatter.format(totalAmount));
        btnContinue.setEnabled(!selectedSeats.isEmpty());
    }

    // Static inner classes for parsing seat_layout JSON
    static class SeatLayout {
        List<Floor> floors;
    }

    static class Floor {
        int floor;
        int rows;
        int cols;
        List<SeatInfo> seats;
    }

    static class SeatInfo {
        String label;
        String type;
        int row;
        int col;
    }
}

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
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.SeatAdapter;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.model.Trip;

public class SeatSelectionActivity extends AppCompatActivity implements SeatAdapter.OnSeatClickListener {

    private Trip trip;
    private RecyclerView floor1RecyclerView, floor2RecyclerView;
    private SeatAdapter floor1Adapter, floor2Adapter;
    private Button btnContinue;
    private TextView tvSubtotal;

    private ArrayList<String> selectedSeats = new ArrayList<>();
    private List<Seat> floor1Seats;
    private List<Seat> floor2Seats;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        trip = getIntent().getParcelableExtra("trip");

        if (trip == null || trip.getSeatLayout() == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin chuyến đi hoặc sơ đồ ghế.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSeatLayout();

        btnContinue.setOnClickListener(v -> {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ghế.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SelectPickupPointActivity.class);
            intent.putExtra("trip", trip);
            intent.putStringArrayListExtra("seat_labels", selectedSeats);
            startActivity(intent);
        });
    }

    private void initViews() {
        floor1RecyclerView = findViewById(R.id.floor1RecyclerView);
        floor2RecyclerView = findViewById(R.id.floor2RecyclerView);
        btnContinue = findViewById(R.id.btnContinue);
        tvSubtotal = findViewById(R.id.tvSubtotal);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupSeatLayout() {
        Gson gson = new Gson();
        Type type = new TypeToken<List<List<Seat>>>() {}.getType();
        List<List<Seat>> seatLayout = gson.fromJson(trip.getSeatLayout(), type);

        if (seatLayout == null || seatLayout.size() != 2) {
            Toast.makeText(this, "Lỗi: Sơ đồ ghế không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        floor1Seats = seatLayout.get(0);
        floor2Seats = seatLayout.get(1);

        floor1Adapter = new SeatAdapter(floor1Seats, this);
        floor2Adapter = new SeatAdapter(floor2Seats, this);

        floor1RecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        floor2RecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        floor1RecyclerView.setAdapter(floor1Adapter);
        floor2RecyclerView.setAdapter(floor2Adapter);
        
        updateSubtotal();
    }

    @Override
    public void onSeatClick(Seat seat) {
        if (seat.isBooked()) {
            return; 
        }

        seat.setSelected(!seat.isSelected());

        if (floor1Seats.contains(seat)) {
            floor1Adapter.notifyItemChanged(floor1Seats.indexOf(seat));
        } else if (floor2Seats.contains(seat)) {
            floor2Adapter.notifyItemChanged(floor2Seats.indexOf(seat));
        }

        if (seat.isSelected()) {
            selectedSeats.add(seat.getLabel());
        } else {
            selectedSeats.remove(seat.getLabel());
        }
        updateSubtotal();
    }

    private void updateSubtotal() {
        double price = 0;
        try {
            price = trip.getPrice();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi định dạng giá vé.", Toast.LENGTH_SHORT).show();
        }
        double subtotal = selectedSeats.size() * price;
        tvSubtotal.setText(String.format(Locale.getDefault(), "%,.0fđ", subtotal));
    }
}

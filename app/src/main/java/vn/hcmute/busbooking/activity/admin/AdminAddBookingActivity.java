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
    private EditText etPassengerName, etPassengerPhone, etPassengerEmail;
    private Spinner spinnerPaymentMethod;
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
        etPassengerName = findViewById(R.id.etPassengerName);
        etPassengerPhone = findViewById(R.id.etPassengerPhone);
        etPassengerEmail = findViewById(R.id.etPassengerEmail);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
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
        apiService.getSeats(tripId, null).enqueue(new Callback<List<Seat>>() {
            @Override
            public void onResponse(Call<List<Seat>> call, Response<List<Seat>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    seatList.clear();
                    seatList.addAll(response.body());
                    seatAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AdminAddBookingActivity.this, "Failed to load seats", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Seat>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAddBookingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSeatCount() {
        int count = selectedSeats.size();
        double totalPrice = tripPrice * count;
        tvSeatCount.setText(String.format(Locale.getDefault(), "%d ghế", count));
        tvTotalPrice.setText(formatPrice(totalPrice));
    }

    private void validateAndCreateBooking() {
        String passengerName = etPassengerName.getText().toString().trim();
        String passengerPhone = etPassengerPhone.getText().toString().trim();
        String passengerEmail = etPassengerEmail.getText().toString().trim();

        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passengerName.isEmpty()) {
            Toast.makeText(this, "Please enter passenger name", Toast.LENGTH_SHORT).show();
            etPassengerName.requestFocus();
            return;
        }

        if (passengerPhone.isEmpty()) {
            Toast.makeText(this, "Please enter passenger phone", Toast.LENGTH_SHORT).show();
            etPassengerPhone.requestFocus();
            return;
        }

        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString().toLowerCase();
        createBooking(passengerName, passengerPhone, passengerEmail, paymentMethod);
    }

    private void createBooking(String name, String phone, String email, String paymentMethod) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> body = new HashMap<>();
        body.put("trip_id", tripId);
        body.put("seat_labels", new ArrayList<>(selectedSeats));
        body.put("passenger_name", name);
        body.put("passenger_phone", phone);
        body.put("passenger_email", email);
        body.put("payment_method", paymentMethod);

        int userId = sessionManager.getUserId();
        apiService.adminCreateBooking(userId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminAddBookingActivity.this, "Booking created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminAddBookingActivity.this, "Failed to create booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAddBookingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private String formatPrice(double price) {
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
        nf.setMaximumFractionDigits(0);
        return nf.format(price) + "đ";
    }
}


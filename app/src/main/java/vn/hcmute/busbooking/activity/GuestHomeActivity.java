package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;

public class GuestHomeActivity extends AppCompatActivity {

    private AutoCompleteTextView etOrigin, etDestination;
    private TextView tvDate;
    private ImageButton btnSwap;
    private Button btnSearchTrips;
    private RecyclerView rvPopularRoutes;
    private BottomNavigationView bottomNav;
    private TextView tvLogin;

    private ApiService apiService;
    private TripAdapter tripAdapter;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_home);

        // --- Find Views ---
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        tvDate = findViewById(R.id.tvDate);
        btnSwap = findViewById(R.id.btnSwap);
        btnSearchTrips = findViewById(R.id.btnSearchTrips);
        rvPopularRoutes = findViewById(R.id.rvPopularRoutes);
        bottomNav = findViewById(R.id.bottom_navigation);
        tvLogin = findViewById(R.id.tvLogin);

        // --- Initialize ApiService ---
        apiService = ApiClient.getClient().create(ApiService.class);

        // --- Setup RecyclerView ---
        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tripAdapter = new TripAdapter(new ArrayList<>());
        rvPopularRoutes.setAdapter(tripAdapter);

        // --- Set Listeners ---
        setupClickListeners();

        // --- Populate Data ---
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);

        fetchTrips();
    }

    private void setupClickListeners() {
        // Date Picker
        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };
        tvDate.setOnClickListener(v -> new DatePickerDialog(GuestHomeActivity.this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show());

        // Swap origin and destination
        btnSwap.setOnClickListener(v -> {
            String origin = etOrigin.getText().toString();
            String destination = etDestination.getText().toString();
            etOrigin.setText(destination);
            etDestination.setText(origin);
        });

        // Search Button
        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString().trim();
            String to = etDestination.getText().toString().trim();
            String dateStr = tvDate.getText().toString().trim();

            if (from.isEmpty() || to.isEmpty() || dateStr.isEmpty() || dateStr.equals("Ngày đi")) {
                Toast.makeText(this, "Vui lòng chọn đủ điểm đi, điểm đến và ngày đi", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
            intent.putExtra("from", from);
            intent.putExtra("to", to);
            intent.putExtra("date", dateStr);
            startActivity(intent);
        });

        // Adapter item click
        tripAdapter.setOnItemClickListener(trip -> {
            DatePickerDialog.OnDateSetListener datePicker = (view, year, month, day) -> {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                String selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(myCalendar.getTime());

                Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
                intent.putExtra("from", trip.getFromLocation());
                intent.putExtra("to", trip.getToLocation());
                intent.putExtra("date", selectedDate);
                startActivity(intent);
            };
            new DatePickerDialog(GuestHomeActivity.this, datePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Login Button
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(GuestHomeActivity.this, LoginActivity.class));
        });

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_tickets || itemId == R.id.nav_account) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void updateLabel() {
        String myFormat = "dd/MM/yyyy"; 
        SimpleDateFormat dateFormat = new SimpleDateFormat(myFormat, Locale.US);
        tvDate.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void fetchTrips() {
        apiService.getTrips(null, null).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tripAdapter.updateTrips(response.body());
                } else {
                    Toast.makeText(GuestHomeActivity.this, "Failed to fetch trips", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Toast.makeText(GuestHomeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("GuestHomeActivity", "Error fetching trips", t);
            }
        });
    }
}

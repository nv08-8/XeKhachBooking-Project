package vn.hcmute.busbooking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import vn.hcmute.busbooking.activity.GuestAccountActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.MyBookingsActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.activity.UserAccountActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin, tvDate; // Added tvDate
    private SessionManager sessionManager;
    private Calendar selectedDate = Calendar.getInstance(); // To store the selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views and SessionManager
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        btnSearchTrips = findViewById(R.id.btnSearchTrips);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLogin = findViewById(R.id.tvLogin);
        tvDate = findViewById(R.id.tvDate); // Find tvDate
        sessionManager = new SessionManager(this);

        // Update UI based on login status
        updateUI();
        updateDateLabel();

        // Setup AutoCompleteTextViews
        String[] locations = {"TP.HCM", "Ha Noi", "Da Nang", "Da Lat", "Nha Trang", "Buon Ma Thuot", "Quy Nhon", "Can Tho", "Vung Tau", "Hue", "Quang Binh", "Thanh Hoa", "Hai Phong"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);

        // Date Picker setup
        tvDate.setOnClickListener(v -> showDatePickerDialog());

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString();
            String to = etDestination.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String date = sdf.format(selectedDate.getTime());

            Intent intent = new Intent(MainActivity.this, TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            intent.putExtra("date", date);
            startActivity(intent);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on the home screen, do nothing
                return true;
            } else if (itemId == R.id.nav_tickets) { // Correct ID
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                return true;
            } else if (itemId == R.id.nav_account) {
                 if (sessionManager.isLoggedIn()) {
                     Intent intent = new Intent(this, UserAccountActivity.class);
                     intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                     startActivity(intent);
                 } else {
                     Intent intent = new Intent(this, GuestAccountActivity.class);
                     intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                     startActivity(intent);
                 }
                 return true;
            } else {
                // Handle other menu items here
                return false;
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        };

        new DatePickerDialog(MainActivity.this,
                dateSetListener,
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateUI() {
        if (sessionManager.isLoggedIn()) {
            // User is logged in
            tvWelcome.setText("Xin chào, " + sessionManager.getUserName() + "!");
            tvLogin.setText("Đăng xuất");
            tvLogin.setOnClickListener(v -> {
                // Perform logout
                sessionManager.logout();
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

                // Restart MainActivity to show guest UI
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            // User is not logged in (Guest mode)
            tvWelcome.setText("Xin chào, Khách!");
            tvLogin.setText("Đăng nhập");
            tvLogin.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }
    }
}

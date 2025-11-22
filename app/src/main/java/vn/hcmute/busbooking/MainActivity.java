package vn.hcmute.busbooking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import vn.hcmute.busbooking.activity.FavoritesActivity;
import vn.hcmute.busbooking.activity.GuestAccountActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.MyBookingsActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.activity.UserAccountActivity;
import vn.hcmute.busbooking.adapter.PopularRoutesAdapter;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.adapter.TestimonialsAdapter;
import vn.hcmute.busbooking.model.PopularRoute;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Testimonial;
import vn.hcmute.busbooking.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin, tvDate; // Added tvDate
    private androidx.appcompat.widget.SwitchCompat switchReturn;
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
        switchReturn = findViewById(R.id.switchReturn);
        sessionManager = new SessionManager(this);

        // Prepare RecyclerViews early (avoid "No adapter attached; skipping layout" warnings)
        RecyclerView rvPopular = findViewById(R.id.rvPopularRoutes);
        RecyclerView rvPromos = findViewById(R.id.rvPromotions);
        RecyclerView rvReviews = findViewById(R.id.rvTestimonials);

        if (rvPopular != null) {
            rvPopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPopular.setHasFixedSize(true);
            rvPopular.setNestedScrollingEnabled(false);
            PopularRoutesAdapter prAdapter = new PopularRoutesAdapter(new ArrayList<>());
            rvPopular.setAdapter(prAdapter);
        }

        if (rvPromos != null) {
            rvPromos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPromos.setHasFixedSize(true);
            rvPromos.setNestedScrollingEnabled(false);
            PromotionsAdapter promoAdapter = new PromotionsAdapter(new ArrayList<>());
            rvPromos.setAdapter(promoAdapter);
        }

        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvReviews.setHasFixedSize(true);
            rvReviews.setNestedScrollingEnabled(false);
            TestimonialsAdapter tAdapter = new TestimonialsAdapter(new ArrayList<>());
            rvReviews.setAdapter(tAdapter);
        }

        // Update UI based on login status
        updateUI();
        updateDateLabel();

        // Setup AutoCompleteTextViews
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột", "Quy Nhơn", "Cần Thơ", "Vũng Tàu", "Huế", "Quảng Bình", "Thanh Hóa", "Hải Phòng"};
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
            boolean isReturn = switchReturn.isChecked();

            Intent intent = new Intent(MainActivity.this, TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            intent.putExtra("date", date);
            intent.putExtra("isReturn", isReturn);
            startActivity(intent);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View navPromoContainer = findViewById(R.id.navPromoContainer);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (navPromoContainer != null) navPromoContainer.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_tickets) {
                if (navPromoContainer != null) navPromoContainer.setVisibility(View.GONE);
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                return true;
            } else if (itemId == R.id.nav_favorites) {
                if (navPromoContainer != null) navPromoContainer.setVisibility(View.GONE);
                Intent intent = new Intent(this, FavoritesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_account) {
                if (navPromoContainer != null) navPromoContainer.setVisibility(View.GONE);
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
            String username = sessionManager.getUserName();
            if (tvWelcome != null) {
                if (username != null && !username.isEmpty()) {
                    tvWelcome.setText("Chào " + username + ",\nbạn muốn đi đâu?");
                } else {
                    tvWelcome.setText("Chào bạn,\nbạn muốn đi đâu?");
                }
            }
            if (tvLogin != null) {
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
            }
        } else {
            // User is not logged in (Guest mode)
            if (tvWelcome != null) tvWelcome.setText("Chào bạn,\nbạn muốn đi đâu?");
            if (tvLogin != null) {
                tvLogin.setText("Đăng nhập");
                tvLogin.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                });
            }
        }
    }
}

package vn.hcmute.busbooking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.text.InputType;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.LocationsMeta;
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.ws.SocketManager;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin, tvDate; // Added tvDate
    private androidx.appcompat.widget.SwitchCompat switchReturn;
    private SessionManager sessionManager;
    private Calendar selectedDate = Calendar.getInstance(); // To store the selected date
    private View mainLayout;
    private View statusBarScrim;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        handleWindowInsets();

        // Initialize views and SessionManager
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        ImageView ivSwap = findViewById(R.id.ivSwap);
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
        // Immediately set a fallback adapter so dropdown works even if backend is down
        String[] fallbackLocations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fallbackLocations);
        etOrigin.setAdapter(initialAdapter);
        etDestination.setAdapter(initialAdapter);

        // Try fetch meta locations from backend
        apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getMetaLocations().enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        java.util.Map<String, Object> body = response.body();
                        java.util.List<String> origins = new java.util.ArrayList<>();
                        java.util.List<String> destinations = new java.util.ArrayList<>();
                        try {
                            Object o1 = body.get("origins");
                            if (o1 instanceof java.util.List) {
                                for (Object it : (java.util.List<?>) o1) if (it != null) origins.add(String.valueOf(it));
                            }
                            Object o2 = body.get("destinations");
                            if (o2 instanceof java.util.List) {
                                for (Object it : (java.util.List<?>) o2) if (it != null) destinations.add(String.valueOf(it));
                            }
                        } catch (Exception ignored) {}
                        java.util.Set<String> union = new java.util.LinkedHashSet<>();
                        union.addAll(origins);
                        union.addAll(destinations);
                        if (union.isEmpty()) {
                            union.add("TP.HCM"); union.add("Hà Nội"); union.add("Đà Nẵng"); union.add("Đà Lạt"); union.add("Nha Trang");
                        }
                        String[] arr = union.toArray(new String[0]);
                        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, arr);
                        etOrigin.setAdapter(newAdapter);
                        etDestination.setAdapter(newAdapter);
                    }
                    // if not successful, leave initialAdapter (fallback) as-is
                });
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                // network failure: do nothing, initialAdapter already set
            }
        });

        // Use inputType null to avoid keyboard but keep touch/click behaviors stable
        etOrigin.setInputType(InputType.TYPE_NULL);
        etDestination.setInputType(InputType.TYPE_NULL);
        etOrigin.setThreshold(0);
        etDestination.setThreshold(0);
        etOrigin.setFocusable(true);
        etOrigin.setFocusableInTouchMode(true);
        etOrigin.setClickable(true);
        etDestination.setFocusable(true);
        etDestination.setFocusableInTouchMode(true);
        etDestination.setClickable(true);
        etOrigin.setOnClickListener(v -> { etOrigin.requestFocus(); etOrigin.showDropDown(); });
        etDestination.setOnClickListener(v -> { etDestination.requestFocus(); etDestination.showDropDown(); });
        etOrigin.setOnTouchListener((v, event) -> {
            etOrigin.requestFocus();
            etOrigin.post(() -> {
                try { etOrigin.showDropDown(); } catch (Exception ignored) {}
            });
            return false; // allow normal handling
        });
        etDestination.setOnTouchListener((v, event) -> {
            etDestination.requestFocus();
            etDestination.post(() -> {
                try { etDestination.showDropDown(); } catch (Exception ignored) {}
            });
            return false;
        });

        // Swap origin/destination when ivSwap clicked
        if (ivSwap != null) {
            ivSwap.setOnClickListener(v -> {
                String from = etOrigin.getText().toString();
                String to = etDestination.getText().toString();
                etOrigin.setText(to);
                etDestination.setText(from);
                // small rotate animation for feedback
                RotateAnimation rotate = new RotateAnimation(0f, 180f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(300);
                ivSwap.startAnimation(rotate);
            });
        }

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

    @Override
    protected void onStart() {
        super.onStart();
        // Connect global socket if user logged in
        if (sessionManager == null) sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            SocketManager.getInstance(this).connect();
        } else {
            // ensure disconnected when not logged in
            SocketManager.getInstance(this).disconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect socket when app is backgrounded/closed
        SocketManager.getInstance(this).disconnect();
    }

    private void handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = statusBarHeight;
            v.setLayoutParams(params);

            if (statusBarScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                scrimParams.height = statusBarHeight;
                statusBarScrim.setLayoutParams(scrimParams);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
                statusBarScrim.setBackgroundColor(ContextCompat.getColor(this, R.color.appBarBackground));
            }
            return insets;
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

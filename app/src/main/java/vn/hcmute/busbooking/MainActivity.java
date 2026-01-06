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
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

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
import java.util.List;
import java.util.Locale;

import vn.hcmute.busbooking.activity.FavoritesActivity;
import vn.hcmute.busbooking.activity.guest.GuestAccountActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.MyBookingsActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.activity.UserAccountActivity;
import vn.hcmute.busbooking.adapter.PopularRoutesAdapter;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.adapter.TestimonialsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PopularRoute;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Testimonial;
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.ws.SocketManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin, tvDate;
    private androidx.appcompat.widget.SwitchCompat switchReturn;
    private RecyclerView rvPopularRoutes, rvPromotions, rvTestimonials;
    private BottomNavigationView bottomNav;
    private View statusBarScrim;
    private PopularRoutesAdapter popularRoutesAdapter;
    private PromotionsAdapter promotionsAdapter;
    private TestimonialsAdapter testimonialsAdapter;
    private ApiService apiService;
    private final Calendar myCalendar = Calendar.getInstance();
    private SessionManager sessionManager;
    private View mainLayout;
    private java.util.Calendar selectedDate = java.util.Calendar.getInstance();
    private java.util.Calendar returnDate = null; // new: store selected return date when round-trip enabled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

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
        mainLayout = findViewById(R.id.mainLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        handleWindowInsets();

        // Prepare RecyclerViews early (avoid "No adapter attached; skipping layout" warnings)
        rvPopularRoutes = findViewById(R.id.rvPopularRoutes);
        rvPromotions = findViewById(R.id.rvPromotions);
        rvTestimonials = findViewById(R.id.rvTestimonials);

        if (rvPopularRoutes != null) {
            rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPopularRoutes.setHasFixedSize(true);
            rvPopularRoutes.setNestedScrollingEnabled(false);
            popularRoutesAdapter = new PopularRoutesAdapter(new ArrayList<>());
            popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
                // Auto-search with today's date when popular route is clicked
                Log.d("MainActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
                boolean isReturn = switchReturn.isChecked();

                // Get today's date formatted as dd/MM/yyyy
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String todayDate = dateFormat.format(Calendar.getInstance().getTime());

                Intent intent = new Intent(MainActivity.this, TripListActivity.class);
                intent.putExtra("origin", origin);
                intent.putExtra("destination", destination);
                intent.putExtra("date", todayDate);
                // if return is enabled and we have a cached returnDate, pass it
                if (isReturn && returnDate != null) {
                    String r = dateFormat.format(returnDate.getTime());
                    intent.putExtra("returnDate", r);
                    // forward return origin/destination (swap)
                    intent.putExtra("returnOrigin", destination);
                    intent.putExtra("returnDestination", origin);
                }
                // Use unified key 'isRoundTrip' so downstream activities know this is a round trip
                intent.putExtra("isRoundTrip", isReturn);
                startActivity(intent);
            });
            rvPopularRoutes.setAdapter(popularRoutesAdapter);
        }

        if (rvPromotions != null) {
            rvPromotions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPromotions.setHasFixedSize(true);
            rvPromotions.setNestedScrollingEnabled(false);
            promotionsAdapter = new PromotionsAdapter(new ArrayList<>());
            rvPromotions.setAdapter(promotionsAdapter);
        }

        if (rvTestimonials != null) {
            rvTestimonials.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvTestimonials.setHasFixedSize(true);
            rvTestimonials.setNestedScrollingEnabled(false);
            testimonialsAdapter = new TestimonialsAdapter(new ArrayList<>());
            rvTestimonials.setAdapter(testimonialsAdapter);
        }

        // Update UI based on login status
        updateUI();
        updateDateLabel();

        // Initialize API service BEFORE calling any fetch methods
        apiService = ApiClient.getClient().create(ApiService.class);

        // Fetch real data from backend API (same as GuestHomeActivity)
        fetchPopularRoutes();
        fetchFeaturedPromotions();
        fetchReviews();

        // Setup AutoCompleteTextViews
        // Immediately set a fallback adapter so dropdown works even if backend is down
        String[] fallbackLocations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fallbackLocations);
        etOrigin.setAdapter(initialAdapter);
        etDestination.setAdapter(initialAdapter);

        // Try fetch meta locations from backend
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
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // signal click for accessibility
                v.performClick();
                try { etOrigin.requestFocus(); etOrigin.showDropDown(); } catch (Exception ignored) {}
                return true; // consumed
            }
            return false;
        });
        etDestination.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                try { etDestination.requestFocus(); etDestination.showDropDown(); } catch (Exception ignored) {}
                return true;
            }
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
        tvDate.setOnClickListener(v -> showDatePickerDialog(false));

        // When round-trip is toggled on, ask user to pick a return date immediately
        switchReturn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // open return date picker
                showDatePickerDialog(true);
            } else {
                // clear return date
                returnDate = null;
                updateDateLabel();
            }
        });

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString().trim();
            String to = etDestination.getText().toString().trim();
            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng chọn điểm đi và điểm đến", Toast.LENGTH_SHORT).show();
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String date = sdf.format(selectedDate.getTime());
            boolean isReturn = switchReturn.isChecked();

            Intent intent = new Intent(MainActivity.this, TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            intent.putExtra("date", date);
            // Use unified key 'isRoundTrip' instead of legacy 'isReturn'
            intent.putExtra("isRoundTrip", isReturn);
            if (isReturn && returnDate != null) {
                intent.putExtra("returnDate", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(returnDate.getTime()));
                // forward swap info to help downstream form the return search
                intent.putExtra("returnOrigin", to);
                intent.putExtra("returnDestination", from);
            }
            startActivity(intent);
        });

        bottomNav = findViewById(R.id.bottom_navigation);
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

    // Show DatePicker; if isReturn==true, pick return date instead of departure
    private void showDatePickerDialog(boolean isReturn) {
        java.util.Calendar cal = isReturn && returnDate != null ? returnDate : selectedDate;
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            if (isReturn) {
                if (returnDate == null) returnDate = java.util.Calendar.getInstance();
                returnDate.set(Calendar.YEAR, year);
                returnDate.set(Calendar.MONTH, month);
                returnDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // Validate return date not before departure
                if (returnDate.before(selectedDate)) {
                    Toast.makeText(MainActivity.this, "Ngày về không thể trước ngày đi", Toast.LENGTH_LONG).show();
                    returnDate = null;
                }
            } else {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // If we already have returnDate ensure it's after depart
                if (returnDate != null && returnDate.before(selectedDate)) {
                    // reset return date so user can pick again
                    returnDate = null;
                }
            }
            updateDateLabel();
        };

        new DatePickerDialog(MainActivity.this,
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure "Home" is selected when returning to this activity
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
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
        // Legacy compatibility: Pick departure if called without param
        showDatePickerDialog(false);
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (switchReturn != null && switchReturn.isChecked() && returnDate != null) {
            String dep = sdf.format(selectedDate.getTime());
            String ret = sdf.format(returnDate.getTime());
            tvDate.setText(String.format(Locale.getDefault(), "%s → %s", dep, ret));
        } else {
            tvDate.setText(sdf.format(selectedDate.getTime()));
        }
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

    private void fetchPopularRoutes() {
        Log.d("MainActivity", "=== fetchPopularRoutes called ===");
        apiService.getPopularRoutes(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                Log.d("MainActivity", "Popular routes response: successful=" + response.isSuccessful() + ", code=" + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    Log.d("MainActivity", "Popular routes count: " + rows.size());
                    java.util.List<PopularRoute> routes = new java.util.ArrayList<>();
                    int imageIndex = 0;
                    int[] routeImages = {
                        R.drawable.img_route1,
                        R.drawable.img_route2,
                        R.drawable.img_route3,
                        R.drawable.img_route4,
                        R.drawable.img_route5
                    };

                    for (java.util.Map<String, Object> r : rows) {
                        final String name = r.get("name") != null ? String.valueOf(r.get("name")) : "";
                        Log.d("MainActivity", "Processing route: " + name);

                        // Parse and format price properly
                        Object priceObj = r.get("avg_price");
                        String priceTemp = "Liên hệ";
                        if (priceObj != null) {
                            try {
                                String priceStr = String.valueOf(priceObj).trim().replace("\"", "");
                                double priceValue = Double.parseDouble(priceStr);
                                priceTemp = "Từ " + String.format("%,.0f", priceValue) + " VND";
                                Log.d("MainActivity", "  Price parsed: " + priceTemp);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error parsing avg_price: " + priceObj, e);
                            }
                        }
                        final String price = priceTemp;

                        // Get image_url directly from API response
                        String imageUrl = r.get("image_url") != null ? String.valueOf(r.get("image_url")) : null;
                        Log.d("MainActivity", "  image_url=" + imageUrl);

                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                            // Use image URL from API
                            Log.d("MainActivity", "  Using image URL from API");
                            routes.add(new PopularRoute(name, price, imageUrl));
                        } else {
                            // No image URL, use fallback drawable
                            Log.d("MainActivity", "  Using fallback drawable");
                            int img = routeImages[imageIndex % routeImages.length];
                            routes.add(new PopularRoute(name, price, img));
                        }
                        Log.d("MainActivity", "  Route added (total now: " + routes.size() + ")");

                        imageIndex++;
                    }

                    // Update adapter with all routes
                    Log.d("MainActivity", "All routes loaded, updating adapter with " + routes.size() + " routes");
                    popularRoutesAdapter = new PopularRoutesAdapter(routes);
                    popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
                        // Auto-search with today's date
                        Log.d("MainActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
                        boolean isReturn = switchReturn.isChecked();

                        // Get today's date formatted as dd/MM/yyyy
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String todayDate = dateFormat.format(Calendar.getInstance().getTime());

                        Intent intent = new Intent(MainActivity.this, TripListActivity.class);
                        intent.putExtra("origin", origin);
                        intent.putExtra("destination", destination);
                        intent.putExtra("date", todayDate);
                        // Use unified key 'isRoundTrip' so downstream activities know this is a round trip
                        intent.putExtra("isRoundTrip", isReturn);
                        // if return is enabled and we have a cached returnDate, pass it
                        if (isReturn && returnDate != null) {
                            String r = dateFormat.format(returnDate.getTime());
                            intent.putExtra("returnDate", r);
                            // forward return origin/destination (swap)
                            intent.putExtra("returnOrigin", destination);
                            intent.putExtra("returnDestination", origin);
                        }
                        startActivity(intent);
                    });
                    rvPopularRoutes.setAdapter(popularRoutesAdapter);
                } else {
                    Log.w("MainActivity", "Popular routes response failed or empty, using mock data");
                    setupMockData();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                Log.e("MainActivity", "Failed to fetch popular routes", t);
                setupMockData();
            }
        });
    }

    private void fetchFeaturedPromotions() {
        apiService.getFeaturedPromotions(5).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    java.util.List<Promotion> promos = new java.util.ArrayList<>();
                    int imageIndex = 0;
                    int[] promoImages = {R.drawable.promo_bg_1, R.drawable.promo_bg_2};

                    for (java.util.Map<String, Object> p : rows) {
                        // Log raw data for debugging
                        Log.d("MainActivity", "Promotion data: " + p.toString());

                        // Generate title and description from promotion data
                        String code = p.get("code") != null ? String.valueOf(p.get("code")) : "";
                        String discountType = p.get("discount_type") != null ? String.valueOf(p.get("discount_type")) : "";

                        // Parse discount_value more robustly
                        Object discountValueObj = p.get("discount_value");
                        double discountValue = 0;
                        if (discountValueObj != null) {
                            try {
                                String valueStr = String.valueOf(discountValueObj).trim();
                                valueStr = valueStr.replace("\"", "");
                                discountValue = Double.parseDouble(valueStr);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error parsing discount_value: '" + discountValueObj + "'", e);
                            }
                        }

                        Log.d("MainActivity", "Parsed values - code: " + code + ", type: " + discountType + ", value: " + discountValue);

                        // Skip if discount value is 0 or invalid
                        if (discountValue <= 0) {
                            Log.w("MainActivity", "Skipping promotion with invalid discount_value: " + code);
                            continue;
                        }

                        // Create title based on discount type and value
                        String title;
                        if ("percent".equalsIgnoreCase(discountType)) {
                            title = "Giảm " + (int)discountValue + "%";
                        } else {
                            title = "Giảm " + String.format("%,.0f", discountValue) + " VND";
                        }

                        // Create description from code
                        String desc = "Mã: " + code;

                        // Get min_price if available
                        Object minPriceObj = p.get("min_price");
                        if (minPriceObj != null) {
                            try {
                                String minPriceStr = String.valueOf(minPriceObj).trim();
                                minPriceStr = minPriceStr.replace("\"", "");
                                double minPrice = Double.parseDouble(minPriceStr);
                                if (minPrice > 0) {
                                    desc += " • Đơn tối thiểu " + String.format("%,.0f", minPrice) + " VND";
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error parsing min_price: '" + minPriceObj + "'", e);
                            }
                        }

                        // Rotate between available promo images
                        int imageRes = promoImages[imageIndex % promoImages.length];
                        imageIndex++;

                        promos.add(new Promotion(title, desc, imageRes));
                    }

                    Log.d("MainActivity", "Loaded " + promos.size() + " valid promotions");

                    if (promos.isEmpty()) {
                        Log.w("MainActivity", "No valid promotions from API, using mock data");
                        setupMockPromotions();
                        return;
                    }

                    promotionsAdapter = new PromotionsAdapter(promos);
                    rvPromotions.setAdapter(promotionsAdapter);
                } else {
                    Log.w("MainActivity", "API response not successful or empty, using mock data");
                    setupMockPromotions();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                Log.e("MainActivity", "Failed to fetch promotions", t);
                setupMockPromotions();
            }
        });
    }

    private void setupMockPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("Giảm 30%", "Mã: SALE30 • Ưu đãi tháng 12", R.drawable.promo_bg_1));
        promotions.add(new Promotion("Giảm 50.000đ", "Mã: GIAM50K • Cuối tuần", R.drawable.promo_bg_2));

        promotionsAdapter = new PromotionsAdapter(promotions);
        rvPromotions.setAdapter(promotionsAdapter);
    }

    private void fetchReviews() {
        apiService.getReviews(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    java.util.List<Testimonial> list = new java.util.ArrayList<>();
                    for (java.util.Map<String, Object> r : rows) {
                        String name = r.get("user_name") != null ? String.valueOf(r.get("user_name")) : "Khách";
                        String comment = r.get("comment") != null ? String.valueOf(r.get("comment")) : "";
                        String route = "";
                        if (r.get("origin") != null && r.get("destination") != null) {
                            route = r.get("origin") + " - " + r.get("destination");
                        }
                        int rating = 5;
                        if (r.get("rating") != null) {
                            try {
                                rating = ((Number) r.get("rating")).intValue();
                            } catch (Exception ignored) {}
                        }
                        list.add(new Testimonial(name, route, comment, R.drawable.user1, rating));
                    }
                    testimonialsAdapter = new TestimonialsAdapter(list);
                    rvTestimonials.setAdapter(testimonialsAdapter);
                } else {
                    // Keep mock testimonials
                    setupMockTestimonials();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                setupMockTestimonials();
            }
        });
    }

    private void setupMockData() {
        List<PopularRoute> routes = new ArrayList<>();
        routes.add(new PopularRoute("TP.HCM - Đà Lạt", "Từ 180.000đ", R.drawable.img_route1));
        routes.add(new PopularRoute("Hà Nội - Hải Phòng", "Từ 120.000đ", R.drawable.img_route2));
        routes.add(new PopularRoute("TP.HCM - Nha Trang", "Từ 199.000đ", R.drawable.img_route3));

        popularRoutesAdapter = new PopularRoutesAdapter(routes);
        popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
            // Auto-search with today's date
            Log.d("MainActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
            boolean isReturn = switchReturn.isChecked();

            // Get today's date formatted as dd/MM/yyyy
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String todayDate = dateFormat.format(Calendar.getInstance().getTime());

            Intent intent = new Intent(MainActivity.this, TripListActivity.class);
            intent.putExtra("origin", origin);
            intent.putExtra("destination", destination);
            intent.putExtra("date", todayDate);
            intent.putExtra("isRoundTrip", isReturn);
        });
        rvPopularRoutes.setAdapter(popularRoutesAdapter);
    }

    private void setupMockTestimonials() {
        List<Testimonial> testimonials = new ArrayList<>();
        testimonials.add(new Testimonial(
            "Anh Nguyễn Tuấn Quỳnh",
            "CEO Saigon Books",
            "\"Lần trước tôi có việc gấp phải đi công tác, lên mạng tìm vé xe thì tình cờ tìm thấy GoUTE. Sau khi tham khảo, tôi quyết định đặt vé và thanh toán. Công nhận rất tiện và nhanh chóng.\"",
            R.drawable.user1
        ));
        testimonials.add(new Testimonial(
            "Chị Trần Minh Hằng",
            "Giám đốc Marketing",
            "\"Tôi thường xuyên đi công tác và GoUTE là ứng dụng đầu tiên tôi nghĩ đến. Giao diện đẹp, dễ sử dụng, và đặc biệt là có nhiều chương trình khuyến mãi hấp dẫn.\"",
            R.drawable.user2
        ));
        testimonials.add(new Testimonial(
            "Anh Lê Hoàng Nam",
            "Kỹ sư phần mềm",
            "\"Mình đã sử dụng GoUTE được 6 tháng và rất hài lòng. Thanh toán online rất tiện lợi, không phải xếp hàng mua vé. Hệ thống chọn ghế cũng rất trực quan.\"",
            R.drawable.user3
        ));
        testimonialsAdapter = new TestimonialsAdapter(testimonials);
        rvTestimonials.setAdapter(testimonialsAdapter);
    }
}

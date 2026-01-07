package vn.hcmute.busbooking.activity.guest;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;
import android.util.TypedValue;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.adapter.PopularRoutesAdapter;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.adapter.TestimonialsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PopularRoute;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Testimonial;

public class GuestHomeActivity extends AppCompatActivity {

    private TextView tvLogin, tvDate;
    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private androidx.appcompat.widget.SwitchCompat switchReturn;
    private RecyclerView rvPopularRoutes, rvPromotions, rvTestimonials;
    private BottomNavigationView bottomNav;
    private View statusBarScrim;
    private View mainLayout;
    private PopularRoutesAdapter popularRoutesAdapter;
    private PromotionsAdapter promotionsAdapter;
    private TestimonialsAdapter testimonialsAdapter;
    private ApiService apiService;
    private final Calendar myCalendar = Calendar.getInstance();
    private java.util.Calendar selectedDate = java.util.Calendar.getInstance();
    private java.util.Calendar returnDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_guest_home);

        // --- Find Views ---
        tvLogin = findViewById(R.id.tvLogin);
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        ImageView ivSwap = findViewById(R.id.ivSwap);
        btnSearchTrips = findViewById(R.id.btnSearchTrips);
        switchReturn = findViewById(R.id.switchReturn);
        rvPopularRoutes = findViewById(R.id.rvPopularRoutes);
        rvPromotions = findViewById(R.id.rvPromotions);
        rvTestimonials = findViewById(R.id.rvTestimonials);
        bottomNav = findViewById(R.id.bottom_navigation);
        tvDate = findViewById(R.id.tvDate);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        mainLayout = findViewById(R.id.mainLayout);
        handleWindowInsets();

        apiService = ApiClient.getClient().create(ApiService.class);

        // --- Set Listeners ---
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(GuestHomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(GuestHomeActivity.this, date, selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        // When round-trip is toggled, immediately pick return date when enabled
        switchReturn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                DatePickerDialog.OnDateSetListener returnListener = (view, year, month, day) -> {
                    if (returnDate == null) returnDate = java.util.Calendar.getInstance();
                    returnDate.set(Calendar.YEAR, year);
                    returnDate.set(Calendar.MONTH, month);
                    returnDate.set(Calendar.DAY_OF_MONTH, day);
                    if (returnDate.before(selectedDate)) {
                        // invalid, reset and alert
                        returnDate = null;
                        android.widget.Toast.makeText(GuestHomeActivity.this, "Ngày về không thể trước ngày đi", android.widget.Toast.LENGTH_LONG).show();
                    }
                    updateLabel();
                };
                new DatePickerDialog(GuestHomeActivity.this, returnListener,
                        selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
            } else {
                returnDate = null;
                updateLabel();
            }
        });

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText() != null ? etOrigin.getText().toString().trim() : "";
            String to = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";
            boolean isReturn = switchReturn.isChecked();

            Log.d("GuestHomeActivity", "Search trips - origin='" + from + "' destination='" + to + "' isReturn=" + isReturn);

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(GuestHomeActivity.this, "Vui lòng chọn điểm đi và điểm đến", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            // Use unified key 'isRoundTrip' instead of legacy 'isReturn'
            intent.putExtra("isRoundTrip", isReturn);
            if (isReturn && returnDate != null) {
                String r = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(returnDate.getTime());
                intent.putExtra("returnDate", r);
                // Also pass departure date if tvDate was changed earlier
                String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime());
                intent.putExtra("date", d);
            } else {
                String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime());
                intent.putExtra("date", d);
            }
            startActivity(intent);
        });

        // Handle Bottom Navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_tickets) {
                Intent intent = new Intent(this, GuestMyBookingsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                Intent intent = new Intent(this, GuestFavoritesActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_account) {
                Intent intent = new Intent(this, GuestAccountActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);

        // --- Populate Data ---
        String[] fallbackLocations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fallbackLocations);
        etOrigin.setAdapter(initialAdapter);
        etDestination.setAdapter(initialAdapter);

        // Programmatically ensure dropdowns can extend outside the CardView / NestedScrollView
        try {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int horizontalMarginDp = 48; // leave some margin on both sides
            int horizontalPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, horizontalMarginDp, getResources().getDisplayMetrics());
            int dropDownWidth = Math.max(screenWidth - horizontalPaddingPx, screenWidth / 2);
            etOrigin.setDropDownWidth(dropDownWidth);
            etDestination.setDropDownWidth(dropDownWidth);
            // CRITICAL: Set maximum dropdown height to prevent ANR from oversized texture
            int maxDropDownHeightDp = 300; // Maximum 300dp height
            int maxDropDownHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxDropDownHeightDp, getResources().getDisplayMetrics());
            // Also ensure it doesn't exceed half the screen height
            maxDropDownHeight = Math.min(maxDropDownHeight, screenHeight / 2);
            etOrigin.setDropDownHeight(maxDropDownHeight);
            etDestination.setDropDownHeight(maxDropDownHeight);
            int verticalOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            etOrigin.setDropDownVerticalOffset(verticalOffset);
            etDestination.setDropDownVerticalOffset(verticalOffset);
        } catch (Exception ignored) {}

        // Try fetch meta locations from backend and update adapters if available
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
                        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(GuestHomeActivity.this, android.R.layout.simple_list_item_1, arr);
                        etOrigin.setAdapter(newAdapter);
                        etDestination.setAdapter(newAdapter);

                        // If either field has focus, show the dropdown again so it repositions with the new adapter
                        etOrigin.post(() -> { try { if (etOrigin.hasFocus()) etOrigin.showDropDown(); } catch (Exception ignored) {} });
                        etDestination.post(() -> { try { if (etDestination.hasFocus()) etDestination.showDropDown(); } catch (Exception ignored) {} });
                    }
                });
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                // network failure: keep initial fallback adapter
            }
        });

        // Make the AutoCompleteTextViews behave like dropdown-only (non-editable) and reliably open dropdown on touch
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
        etOrigin.setOnClickListener(v -> { etOrigin.requestFocus(); adjustDropDownPosition(etOrigin); etOrigin.showDropDown(); });
        etDestination.setOnClickListener(v -> { etDestination.requestFocus(); adjustDropDownPosition(etDestination); etDestination.showDropDown(); });
        etOrigin.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
                etOrigin.requestFocus();
                etOrigin.post(() -> { try { adjustDropDownPosition(etOrigin); etOrigin.showDropDown(); } catch (Exception ignored) {} });
                return true;
            }
            return false;
        });
        etDestination.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
                etDestination.requestFocus();
                etDestination.post(() -> { try { adjustDropDownPosition(etDestination); etDestination.showDropDown(); } catch (Exception ignored) {} });
                return true;
            }
            return false;
        });
        etOrigin.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) { adjustDropDownPosition(etOrigin); etOrigin.showDropDown(); } });
        etDestination.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) { adjustDropDownPosition(etDestination); etDestination.showDropDown(); } });

        // Swap origin/destination when ivSwap clicked
        if (ivSwap != null) {
            ivSwap.setOnClickListener(v -> {
                String from = etOrigin.getText().toString();
                String to = etDestination.getText().toString();
                etOrigin.setText(to);
                etDestination.setText(from);
                RotateAnimation rotate = new RotateAnimation(0f, 180f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(300);
                ivSwap.startAnimation(rotate);
            });
        }

        // Initialize recyclers with empty adapters and then fetch real data
        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPromotions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTestimonials.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        popularRoutesAdapter = new PopularRoutesAdapter(new java.util.ArrayList<>());
        popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
            // Auto-search with today's date when popular route is clicked
            Log.d("GuestHomeActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
            boolean isReturn = switchReturn.isChecked();

            // Get today's date formatted as dd/MM/yyyy
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String todayDate = dateFormat.format(Calendar.getInstance().getTime());

            Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
            intent.putExtra("origin", origin);
            intent.putExtra("destination", destination);
            intent.putExtra("date", todayDate);
            // Use unified key 'isRoundTrip' instead of legacy 'isReturn'
            intent.putExtra("isRoundTrip", isReturn);
            startActivity(intent);
        });
        promotionsAdapter = new PromotionsAdapter(new java.util.ArrayList<>());
        testimonialsAdapter = new TestimonialsAdapter(new java.util.ArrayList<>());

        rvPopularRoutes.setAdapter(popularRoutesAdapter);
        rvPromotions.setAdapter(promotionsAdapter);
        rvTestimonials.setAdapter(testimonialsAdapter);

        // Fetch from backend
        fetchPopularRoutes();
        fetchFeaturedPromotions();
        fetchReviews();
    }

    private void updateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (switchReturn != null && switchReturn.isChecked() && returnDate != null) {
            tvDate.setText(sdf.format(selectedDate.getTime()) + " → " + sdf.format(returnDate.getTime()));
        } else {
            tvDate.setText(sdf.format(selectedDate.getTime()));
        }
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

    private void fetchPopularRoutes() {
        Log.d("GuestHomeActivity", "=== fetchPopularRoutes called ===");
        apiService.getPopularRoutes(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                Log.d("GuestHomeActivity", "Popular routes response: successful=" + response.isSuccessful() + ", code=" + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    Log.d("GuestHomeActivity", "Popular routes count: " + rows.size());
                    java.util.List<vn.hcmute.busbooking.model.PopularRoute> routes = new java.util.ArrayList<>();
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
                        Log.d("GuestHomeActivity", "Processing route: " + name);

                        // Parse and format price properly
                        Object priceObj = r.get("avg_price");
                        String priceTemp = "Liên hệ";
                        if (priceObj != null) {
                            try {
                                String priceStr = String.valueOf(priceObj).trim().replace("\"", "");
                                double priceValue = Double.parseDouble(priceStr);
                                priceTemp = "Từ " + String.format("%,.0f", priceValue) + " VND";
                                Log.d("GuestHomeActivity", "  Price parsed: " + priceTemp);
                            } catch (Exception e) {
                                Log.e("GuestHomeActivity", "Error parsing avg_price: " + priceObj, e);
                            }
                        }
                        final String price = priceTemp;

                        // Get image_url directly from API response
                        String imageUrl = r.get("image_url") != null ? String.valueOf(r.get("image_url")) : null;
                        Log.d("GuestHomeActivity", "  image_url=" + imageUrl);

                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                            // Use image URL from API
                            Log.d("GuestHomeActivity", "  Using image URL from API");
                            routes.add(new vn.hcmute.busbooking.model.PopularRoute(name, price, imageUrl));
                        } else {
                            // No image URL, use fallback drawable
                            Log.d("GuestHomeActivity", "  Using fallback drawable");
                            int img = routeImages[imageIndex % routeImages.length];
                            routes.add(new vn.hcmute.busbooking.model.PopularRoute(name, price, img));
                        }
                        Log.d("GuestHomeActivity", "  Route added (total now: " + routes.size() + ")");

                        imageIndex++;
                    }

                    // Update adapter with all routes
                    Log.d("GuestHomeActivity", "All routes loaded, updating adapter with " + routes.size() + " routes");
                    popularRoutesAdapter = new PopularRoutesAdapter(routes);
                    popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
                        // Auto-search with today's date
                        Log.d("GuestHomeActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
                        boolean isReturn = switchReturn.isChecked();

                        // Get today's date formatted as dd/MM/yyyy
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String todayDate = dateFormat.format(Calendar.getInstance().getTime());

                        Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
                        intent.putExtra("origin", origin);
                        intent.putExtra("destination", destination);
                        intent.putExtra("date", todayDate);
                        // Use unified key 'isRoundTrip' instead of legacy 'isReturn'
                        intent.putExtra("isRoundTrip", isReturn);
                        startActivity(intent);
                     });
                     rvPopularRoutes.setAdapter(popularRoutesAdapter);
                 } else {
                    Log.w("GuestHomeActivity", "Popular routes response failed or empty: successful=" + response.isSuccessful() + ", body=" + (response.body() != null ? response.body().size() : "null"));
                    // fallback to mock data
                    setupMockData();
                 }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                Log.e("GuestHomeActivity", "Failed to fetch popular routes", t);
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
                    java.util.List<vn.hcmute.busbooking.model.Promotion> promos = new java.util.ArrayList<>();
                    int imageIndex = 0;
                    int[] promoImages = {R.drawable.promo1, R.drawable.promo2};

                    for (java.util.Map<String, Object> p : rows) {
                        // Log raw data for debugging
                        Log.d("GuestHomeActivity", "Promotion data: " + p.toString());

                        // Generate title and description from promotion data
                        String code = p.get("code") != null ? String.valueOf(p.get("code")) : "";
                        String discountType = p.get("discount_type") != null ? String.valueOf(p.get("discount_type")) : "";

                        // Parse discount_value more robustly
                        Object discountValueObj = p.get("discount_value");
                        double discountValue = 0;
                        if (discountValueObj != null) {
                            try {
                                String valueStr = String.valueOf(discountValueObj).trim();
                                // Remove any quotes if present
                                valueStr = valueStr.replace("\"", "");
                                discountValue = Double.parseDouble(valueStr);
                            } catch (Exception e) {
                                Log.e("GuestHomeActivity", "Error parsing discount_value: '" + discountValueObj + "'", e);
                            }
                        }

                        Log.d("GuestHomeActivity", "Parsed values - code: " + code + ", type: " + discountType + ", value: " + discountValue);

                        // Skip if discount value is 0 or invalid
                        if (discountValue <= 0) {
                            Log.w("GuestHomeActivity", "Skipping promotion with invalid discount_value: " + code);
                            continue;
                        }

                        // Create title based on discount type and value
                        String title;
                        if ("percent".equalsIgnoreCase(discountType)) {
                            title = "Giảm " + (int)discountValue + "%";
                        } else {
                            // Format as currency (VND)
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
                                Log.e("GuestHomeActivity", "Error parsing min_price: '" + minPriceObj + "'", e);
                            }
                        }

                        // Rotate between available promo images
                        int imageRes = promoImages[imageIndex % promoImages.length];
                        imageIndex++;

                        promos.add(new vn.hcmute.busbooking.model.Promotion(title, desc, imageRes));
                    }

                    Log.d("GuestHomeActivity", "Loaded " + promos.size() + " valid promotions");

                    // If no valid promotions loaded, show mock data
                    if (promos.isEmpty()) {
                        Log.w("GuestHomeActivity", "No valid promotions from API, using mock data");
                        setupMockPromotions();
                        return;
                    }

                    promotionsAdapter = new PromotionsAdapter(promos);
                    rvPromotions.setAdapter(promotionsAdapter);
                } else {
                    Log.w("GuestHomeActivity", "API response not successful or empty, using mock data");
                    setupMockPromotions();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                Log.e("GuestHomeActivity", "Failed to fetch promotions", t);
                setupMockPromotions();
            }
        });
    }

    private void setupMockPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("Giảm 30%", "Mã: SALE30 • Ưu đãi tháng 12", R.drawable.promo1));
        promotions.add(new Promotion("Giảm 50.000đ", "Mã: GIAM50K • Cuối tuần", R.drawable.promo2));

        promotionsAdapter = new PromotionsAdapter(promotions);
        rvPromotions.setAdapter(promotionsAdapter);
    }

    private void fetchReviews() {
        apiService.getReviews(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    java.util.List<vn.hcmute.busbooking.model.Testimonial> list = new java.util.ArrayList<>();
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
                        list.add(new vn.hcmute.busbooking.model.Testimonial(name, route, comment, R.drawable.user1, rating));
                    }
                    testimonialsAdapter = new TestimonialsAdapter(list);
                    rvTestimonials.setAdapter(testimonialsAdapter);
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                // keep mock testimonials
            }
        });
    }

    private void setupMockData() {
        List<PopularRoute> routes = new ArrayList<>();
        routes.add(new PopularRoute("Hồ Chí Minh – Nha Trang", "Từ 199.000đ", R.drawable.img_route1));
        routes.add(new PopularRoute("Hồ Chí Minh – Đà Lạt", "Từ 150.000đ", R.drawable.img_route2));
        routes.add(new PopularRoute("Hồ Chí Minh – Vũng Tàu", "Từ 120.000đ", R.drawable.img_route3));

        popularRoutesAdapter = new PopularRoutesAdapter(routes);
        popularRoutesAdapter.setOnRouteClickListener((origin, destination) -> {
            // Auto-search with today's date
            Log.d("GuestHomeActivity", "Popular route clicked - Auto-searching: " + origin + " -> " + destination);
            boolean isReturn = switchReturn.isChecked();

            // Get today's date formatted as dd/MM/yyyy
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String todayDate = dateFormat.format(Calendar.getInstance().getTime());

            Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
            intent.putExtra("origin", origin);
            intent.putExtra("destination", destination);
            intent.putExtra("date", todayDate);
            // Use unified key 'isRoundTrip' instead of legacy 'isReturn'
            intent.putExtra("isRoundTrip", isReturn);
            startActivity(intent);
        });
        rvPopularRoutes.setAdapter(popularRoutesAdapter);

        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("Giảm 30%", "Ưu đãi tháng 11", R.drawable.promo1));
        promotions.add(new Promotion("Giảm 50%", "Cuối tuần", R.drawable.promo2));

        promotionsAdapter = new PromotionsAdapter(promotions);
        rvPromotions.setAdapter(promotionsAdapter);

        List<Testimonial> testimonials = new ArrayList<>();
        testimonials.add(new Testimonial("Nguyễn Văn A", "TP.HCM - Nha Trang", "Xe sạch, tài xế vui vẻ!", R.drawable.user1, 5));
        testimonials.add(new Testimonial("Trần Thị B", "TP.HCM - Đà Lạt", "Giá ok, đặt vé nhanh chóng!", R.drawable.user2, 5));

        testimonialsAdapter = new TestimonialsAdapter(testimonials);
        rvTestimonials.setAdapter(testimonialsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning to this activity, ensure the "Home" tab is selected.
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // Adjust dropdown position so it can show above the view when space below is limited
    private void adjustDropDownPosition(AutoCompleteTextView actv) {
        try {
            int[] loc = new int[2];
            actv.getLocationOnScreen(loc);
            int y = loc[1];
            int h = actv.getHeight();
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            // Use fixed max height to prevent ANR
            int maxDropDownHeightDp = 300;
            int maxDropDownHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxDropDownHeightDp, getResources().getDisplayMetrics());
            maxDropDownHeight = Math.min(maxDropDownHeight, screenHeight / 2);
            int desired = actv.getDropDownHeight() > 0 ? Math.min(actv.getDropDownHeight(), maxDropDownHeight) : maxDropDownHeight;
            int spaceBelow = screenHeight - (y + h);
            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            if (spaceBelow < desired + margin) {
                // Not enough space below -> move popup upward so it fits above the field
                int need = (desired + margin) - spaceBelow;
                actv.setDropDownVerticalOffset(-need);
            } else {
                // Enough space below -> normal small offset
                actv.setDropDownVerticalOffset(margin);
            }
        } catch (Exception ignored) {}
    }
}

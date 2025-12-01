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

    private PopularRoutesAdapter popularRoutesAdapter;
    private PromotionsAdapter promotionsAdapter;
    private TestimonialsAdapter testimonialsAdapter;

    private ApiService apiService;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_guest_home);

        statusBarScrim = findViewById(R.id.statusBarScrim);
        View mainLayout = findViewById(R.id.contentScrollView);
        handleWindowInsets(mainLayout);

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

        apiService = ApiClient.getClient().create(ApiService.class);

        // --- Set Listeners ---
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(GuestHomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(GuestHomeActivity.this, date, myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
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
            intent.putExtra("isReturn", isReturn);
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
            etOrigin.setDropDownAnchor(R.id.mainLayout);
            etDestination.setDropDownAnchor(R.id.mainLayout);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int horizontalMarginDp = 48; // leave some margin on both sides
            int horizontalPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, horizontalMarginDp, getResources().getDisplayMetrics());
            int dropDownWidth = Math.max(screenWidth - horizontalPaddingPx, screenWidth / 2);
            etOrigin.setDropDownWidth(dropDownWidth);
            etDestination.setDropDownWidth(dropDownWidth);
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
            etOrigin.requestFocus();
            etOrigin.post(() -> { try { adjustDropDownPosition(etOrigin); etOrigin.showDropDown(); } catch (Exception ignored) {} });
            return false;
        });
        etDestination.setOnTouchListener((v, event) -> {
            etDestination.requestFocus();
            etDestination.post(() -> { try { adjustDropDownPosition(etDestination); etDestination.showDropDown(); } catch (Exception ignored) {} });
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
        String myFormat = "dd/MM/yy"; //In which you need put here
        SimpleDateFormat dateFormat = new SimpleDateFormat(myFormat, Locale.US);
        tvDate.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void handleWindowInsets(View target) {
        ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight + v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
            if (statusBarScrim != null) {
                ViewGroup.LayoutParams params = statusBarScrim.getLayoutParams();
                params.height = statusBarHeight;
                statusBarScrim.setLayoutParams(params);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });
    }

    private void fetchPopularRoutes() {
        apiService.getPopularRoutes(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    java.util.List<vn.hcmute.busbooking.model.PopularRoute> routes = new java.util.ArrayList<>();
                    for (java.util.Map<String, Object> r : rows) {
                        String name = r.get("name") != null ? String.valueOf(r.get("name")) : "";
                        Object priceObj = r.get("avg_price");
                        String price = priceObj != null ? "Từ " + priceObj.toString() : "";
                        // Use a default drawable for server-provided routes
                        int img = R.drawable.img_route1;
                        routes.add(new vn.hcmute.busbooking.model.PopularRoute(name, price, img));
                    }
                    popularRoutesAdapter = new PopularRoutesAdapter(routes);
                    rvPopularRoutes.setAdapter(popularRoutesAdapter);
                } else {
                    // fallback to mock data
                    setupMockData();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
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
                    for (java.util.Map<String, Object> p : rows) {
                        String title = p.get("title") != null ? String.valueOf(p.get("title")) : "";
                        String desc = p.get("description") != null ? String.valueOf(p.get("description")) : "";
                        promos.add(new vn.hcmute.busbooking.model.Promotion(title, desc, R.drawable.promo1));
                    }
                    promotionsAdapter = new PromotionsAdapter(promos);
                    rvPromotions.setAdapter(promotionsAdapter);
                }
            }

            @Override
            public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                // Leave existing (or mock) promotions
            }
        });
    }

    private void fetchReviews() {
        apiService.getReviews(10).enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> rows = response.body();
                    java.util.List<vn.hcmute.busbooking.model.Testimonial> list = new java.util.ArrayList<>();
                    for (java.util.Map<String, Object> r : rows) {
                        String name = r.get("customer_name") != null ? String.valueOf(r.get("customer_name")) : "Khách";
                        String comment = r.get("comment") != null ? String.valueOf(r.get("comment")) : "";
                        list.add(new vn.hcmute.busbooking.model.Testimonial(name, comment, R.drawable.user1));
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
        rvPopularRoutes.setAdapter(popularRoutesAdapter);

        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("Giảm 30%", "Ưu đãi tháng 11", R.drawable.promo1));
        promotions.add(new Promotion("Giảm 50%", "Cuối tuần", R.drawable.promo2));

        promotionsAdapter = new PromotionsAdapter(promotions);
        rvPromotions.setAdapter(promotionsAdapter);

        List<Testimonial> testimonials = new ArrayList<>();
        testimonials.add(new Testimonial("Nguyễn Văn A", "Xe sạch, tài xế vui vẻ!", R.drawable.user1));
        testimonials.add(new Testimonial("Trần Thị B", "Giá ok, đặt vé nhanh chóng!", R.drawable.user2));

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
            int desired = actv.getDropDownHeight() > 0 ? actv.getDropDownHeight() : (screenHeight / 3);
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

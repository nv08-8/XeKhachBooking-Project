package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.PopularRoutesAdapter;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.adapter.TestimonialsAdapter;
import vn.hcmute.busbooking.model.PopularRoute;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Testimonial;

public class GuestHomeActivity extends AppCompatActivity {

    private TextView tvLogin;
    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private RecyclerView rvPopularRoutes, rvPromotions, rvTestimonials;
    private BottomNavigationView bottomNav;

    private PopularRoutesAdapter popularRoutesAdapter;
    private PromotionsAdapter promotionsAdapter;
    private TestimonialsAdapter testimonialsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_home);

        // --- Find Views ---
        tvLogin = findViewById(R.id.tvLogin);
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        btnSearchTrips = findViewById(R.id.btnSearchTrips);
        rvPopularRoutes = findViewById(R.id.rvPopularRoutes);
        rvPromotions = findViewById(R.id.rvPromotions);
        rvTestimonials = findViewById(R.id.rvTestimonials);
        bottomNav = findViewById(R.id.bottom_navigation);

        // --- Set Listeners ---
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(GuestHomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString();
            String to = etDestination.getText().toString();

            Intent intent = new Intent(GuestHomeActivity.this, TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            startActivity(intent);
        });

        // Handle Bottom Navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on the home screen, do nothing
                return true;
            } else if (itemId == R.id.nav_account || itemId == R.id.nav_tickets) {
                // For guests, both "Account" and "My Tickets" go to the GuestAccountActivity
                Intent intent = new Intent(this, GuestAccountActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return false; // Do not keep the item selected as we are navigating away
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);

        // --- Populate Data ---
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);

        setupMockData();
    }

    private void setupMockData() {
        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<PopularRoute> routes = new ArrayList<>();
        routes.add(new PopularRoute("Hồ Chí Minh – Nha Trang", "Từ 199.000đ", R.drawable.img_route1));
        routes.add(new PopularRoute("Hồ Chí Minh – Đà Lạt", "Từ 150.000đ", R.drawable.img_route2));
        routes.add(new PopularRoute("Hồ Chí Minh – Vũng Tàu", "Từ 120.000đ", R.drawable.img_route3));

        popularRoutesAdapter = new PopularRoutesAdapter(routes);
        rvPopularRoutes.setAdapter(popularRoutesAdapter);

        rvPromotions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("Giảm 30%", "Ưu đãi tháng 11", R.drawable.promo1));
        promotions.add(new Promotion("Giảm 50%", "Cuối tuần", R.drawable.promo2));

        promotionsAdapter = new PromotionsAdapter(promotions);
        rvPromotions.setAdapter(promotionsAdapter);

        rvTestimonials.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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
}

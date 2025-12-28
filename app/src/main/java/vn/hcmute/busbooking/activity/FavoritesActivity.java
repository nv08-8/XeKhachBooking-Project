package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.guest.GuestAccountActivity;
import vn.hcmute.busbooking.adapter.TripAdapter;
import vn.hcmute.busbooking.database.FavoriteTripDatabase;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class FavoritesActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private RecyclerView rvFavorites;
    private TextView tvFavoritesEmpty;
    private TripAdapter tripAdapter;
    private AppBarLayout appBarLayout;
    private View statusBarScrim;
    private FavoriteTripDatabase favoriteTripDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_favorites);

        appBarLayout = findViewById(R.id.appBarLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        handleWindowInsets();

        sessionManager = new SessionManager(this);
        favoriteTripDatabase = new FavoriteTripDatabase(this);

        rvFavorites = findViewById(R.id.rvFavorites);
        if (rvFavorites != null) {
            rvFavorites.setLayoutManager(new LinearLayoutManager(this));
            rvFavorites.setHasFixedSize(true);
            tripAdapter = new TripAdapter(this, new ArrayList<>());
            rvFavorites.setAdapter(tripAdapter);
        }
        tvFavoritesEmpty = findViewById(R.id.tvFavoritesEmpty);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_favorites);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_tickets) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_favorites) {
                    return true;
                } else if (itemId == R.id.nav_account) {
                    Intent intent;
                    if (sessionManager.isLoggedIn()) {
                        intent = new Intent(this, UserAccountActivity.class);
                    } else {
                        intent = new Intent(this, GuestAccountActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = statusBarHeight;
            v.setLayoutParams(params);
            if (statusBarScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                scrimParams.height = statusBarHeight;
                statusBarScrim.setLayoutParams(scrimParams);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteTrips();
    }

    private void loadFavoriteTrips() {
        if (favoriteTripDatabase == null || rvFavorites == null || tripAdapter == null) {
            return;
        }
        List<Trip> favoriteTrips = favoriteTripDatabase.getAllFavoriteTrips();

        if (favoriteTrips.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            tvFavoritesEmpty.setVisibility(View.VISIBLE);
            tripAdapter.updateTrips(new ArrayList<>());
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            tvFavoritesEmpty.setVisibility(View.GONE);
            tripAdapter.updateTrips(favoriteTrips);
        }
    }
}

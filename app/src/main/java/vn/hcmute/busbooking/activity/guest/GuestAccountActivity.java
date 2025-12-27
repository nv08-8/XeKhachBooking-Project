package vn.hcmute.busbooking.activity.guest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;

public class GuestAccountActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Button btnLogin, btnRegister;
    private View statusBarScrim;
    private NestedScrollView contentScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_guest_account);

        bottomNav = findViewById(R.id.bottom_navigation);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        contentScroll = findViewById(R.id.contentScroll);

        applyWindowInsets();

        // Set the selected item
        int selectedItemId = getIntent().getIntExtra("SELECTED_ITEM", R.id.nav_account);
        bottomNav.setSelectedItemId(selectedItemId);


        // Handle Bottom Navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), GuestHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, GuestMyBookingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, GuestFavoritesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                return true;
            }
            return false;
        });

        // Handle button clicks
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(GuestAccountActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(GuestAccountActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
        }
    }

    private void applyWindowInsets() {
        if (contentScroll != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentScroll, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset + v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
                if (statusBarScrim != null) {
                    ViewGroup.LayoutParams params = statusBarScrim.getLayoutParams();
                    params.height = topInset;
                    statusBarScrim.setLayoutParams(params);
                    statusBarScrim.setVisibility(topInset > 0 ? View.VISIBLE : View.GONE);
                }
                return insets;
            });
        }
    }
}

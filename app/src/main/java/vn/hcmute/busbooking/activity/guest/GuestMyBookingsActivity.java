package vn.hcmute.busbooking.activity.guest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;

public class GuestMyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "GuestMyBookingsActivity";

    private NestedScrollView contentScroll;
    private View statusBarScrim;
    private BottomNavigationView bottomNav;
    private Button btnLogin, btnRegister; // added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_guest_my_bookings);

        contentScroll = findViewById(R.id.contentScroll);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        bottomNav = findViewById(R.id.bottom_navigation);
        handleWindowInsets();

        // Initialize guest buttons (they exist in the layout but previously had no handlers)
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        Log.d(TAG, "onCreate - btnLogin=" + (btnLogin != null) + ", btnRegister=" + (btnRegister != null));

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Log.d(TAG, "btnLogin clicked");
                startActivity(new Intent(GuestMyBookingsActivity.this, LoginActivity.class));
            });
        }

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                Log.d(TAG, "btnRegister clicked");
                startActivity(new Intent(GuestMyBookingsActivity.this, RegisterActivity.class));
            });
        }

        // Handle Bottom Navigation clicks
        bottomNav.setSelectedItemId(R.id.nav_tickets);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(GuestMyBookingsActivity.this, GuestHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_tickets) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(GuestMyBookingsActivity.this, GuestFavoritesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(GuestMyBookingsActivity.this, GuestAccountActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void handleWindowInsets() {
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

package vn.hcmute.busbooking.activity.guest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;

public class GuestFavoritesActivity extends AppCompatActivity {

    private View statusBarScrim;
    private NestedScrollView contentScroll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_guest_favorites);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        contentScroll = findViewById(R.id.contentScroll);

        applyWindowInsets();

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        bottomNav.setSelectedItemId(R.id.nav_favorites);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            } else if (id == R.id.nav_favorites) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, GuestAccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
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

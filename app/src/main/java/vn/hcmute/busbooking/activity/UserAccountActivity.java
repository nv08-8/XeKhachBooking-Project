package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_account);

        //--- INITIALIZE VIEWS ---
        SessionManager sessionManager = new SessionManager(this);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        TextView tvPersonalInfo = findViewById(R.id.tvPersonalInfo);
        TextView tvChangePassword = findViewById(R.id.tvChangePassword);
        TextView tvLogout = findViewById(R.id.tvLogout);
        TextView tvHelpCenter = findViewById(R.id.tvHelpCenter);
        TextView tvTerms = findViewById(R.id.tvTerms);
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        //--- SET USER DATA ---
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getUserEmail());

        //--- CLICK LISTENERS ---

        // ** THE FIX IS TO KEEP THIS SIMPLE **
        tvPersonalInfo.setOnClickListener(v -> {
            startActivity(new Intent(UserAccountActivity.this, PersonalInfoActivity.class));
        });

        tvChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(UserAccountActivity.this, ChangePasswordActivity.class));
        });

        tvLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserAccountActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvHelpCenter.setOnClickListener(v -> startActivity(new Intent(this, HelpCenterActivity.class)));
        tvTerms.setOnClickListener(v -> startActivity(new Intent(this, TermsServiceActivity.class)));
        tvPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        //--- BOTTOM NAVIGATION (Standard Behavior) ---
        bottomNav.setSelectedItemId(R.id.nav_account);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, MyBookingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_account) {
                return true; // Already here, do nothing
            }
            return false;
        });

        //--- INSETS HANDLING ---
        handleWindowInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data
        SessionManager sessionManager = new SessionManager(this);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getUserEmail());

        // Re-select the correct item when returning to this activity
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
        }
    }

    private void handleWindowInsets() {
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        View statusBarScrim = findViewById(R.id.statusBarScrim);
        if (appBarLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).topMargin = statusBarHeight;
                if (statusBarScrim != null) {
                    ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                    scrimParams.height = statusBarHeight;
                    statusBarScrim.setLayoutParams(scrimParams);
                }
                return insets;
            });
        }
    }
}
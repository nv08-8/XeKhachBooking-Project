package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

    private TextView tvUserName, tvUserEmail, tvLogout, tvHelpCenter, tvTerms, tvPrivacyPolicy, tvChangePassword;
    private Button btnEditProfile;
    private SessionManager sessionManager;
    private AppBarLayout appBarLayout;
    private View statusBarScrim;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_account);

        appBarLayout = findViewById(R.id.appBarLayout);
        statusBarScrim = findViewById(R.id.statusBarScrim);
        handleWindowInsets();

        sessionManager = new SessionManager(this);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvLogout = findViewById(R.id.tvLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvChangePassword = findViewById(R.id.tvChangePassword);
        tvHelpCenter = findViewById(R.id.tvHelpCenter);
        tvTerms = findViewById(R.id.tvTerms);
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);

        // Set user info
        // Safely set user info (views may be missing in some layout variants)
        if (tvUserName != null) tvUserName.setText(sessionManager.getUserName() != null ? sessionManager.getUserName() : "");
        if (tvUserEmail != null) tvUserEmail.setText(sessionManager.getUserEmail() != null ? sessionManager.getUserEmail() : "");

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                sessionManager.logout();
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(UserAccountActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }


        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(UserAccountActivity.this, EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (tvChangePassword != null) {
            tvChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(UserAccountActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
            });
        }

        tvHelpCenter.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpCenterActivity.class);
            startActivity(intent);
        });
        tvTerms.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsServiceActivity.class);
            startActivity(intent);
        });
        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });


        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
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
                    Intent intent = new Intent(this, FavoritesActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_account) {
                    return true;
                } else {
                    return false;
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(UserAccountActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
        }
    }

    private void openLegalContent(String title, String content) {
        Intent intent = new Intent(this, LegalContentActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        startActivity(intent);
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
}
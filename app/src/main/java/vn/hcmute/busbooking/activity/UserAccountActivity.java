package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_account);

        //--- INITIALIZE VIEWS ---
        SessionManager sessionManager = new SessionManager(this);
        ImageView ivUserAvatar = findViewById(R.id.ivUserAvatar);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        
        // tvBookingHistory and tvPaymentMethods were removed from the layout; don't reference their IDs
        TextView tvMyOffers = findViewById(R.id.tvMyOffers);
        TextView tvMyFeedbacks = findViewById(R.id.tvMyFeedbacks);
        TextView tvMyCoins = findViewById(R.id.tvMyCoins);
        
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

        //--- LOAD AVATAR IMAGE ---
        loadAvatarImage(ivUserAvatar, sessionManager);

        //--- CLICK LISTENERS ---

        if (tvMyOffers != null) {
            tvMyOffers.setOnClickListener(v -> {
                startActivity(new Intent(UserAccountActivity.this, UserPromotionsActivity.class));
            });
        }

        if (tvMyFeedbacks != null) {
            tvMyFeedbacks.setOnClickListener(v -> {
                startActivity(new Intent(UserAccountActivity.this, FeedbackManagementActivity.class));
            });
        }

        if (tvMyCoins != null) {
            tvMyCoins.setOnClickListener(v -> {
                startActivity(new Intent(UserAccountActivity.this, MyCoinsActivity.class));
            });
        }

        if (tvPersonalInfo != null) {
            tvPersonalInfo.setOnClickListener(v -> {
                startActivity(new Intent(UserAccountActivity.this, PersonalInfoActivity.class));
            });
        }

        if (tvChangePassword != null) {
            tvChangePassword.setOnClickListener(v -> {
                startActivity(new Intent(UserAccountActivity.this, ChangePasswordActivity.class));
            });
        }

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

        if (tvHelpCenter != null) tvHelpCenter.setOnClickListener(v -> startActivity(new Intent(this, HelpCenterActivity.class)));
        if (tvTerms != null) tvTerms.setOnClickListener(v -> startActivity(new Intent(this, TermsServiceActivity.class)));
        if (tvPrivacyPolicy != null) tvPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        //--- BOTTOM NAVIGATION ---
        if (bottomNav != null) {
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
                    return true;
                }
                return false;
            });
        }

        handleWindowInsets();
    }

    private void showPaymentMethodsDialog() {
        String[] methods = {
            getString(R.string.pm_card),
            getString(R.string.pm_qr),
            getString(R.string.pm_cash)
        };
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.label_payment_methods)
                .setItems(methods, null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessionManager = new SessionManager(this);
        ImageView ivUserAvatar = findViewById(R.id.ivUserAvatar);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getUserEmail());

        // Reload avatar image when returning from other activities
        loadAvatarImage(ivUserAvatar, sessionManager);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
        }
    }

    private void loadAvatarImage(ImageView ivUserAvatar, SessionManager sessionManager) {
        if (ivUserAvatar == null) return;

        String avatarUrl = sessionManager.getUserAvatar();

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // If URL is relative (starts with /), prepend API base URL
            String fullUrl = avatarUrl;
            if (avatarUrl.startsWith("/")) {
                String baseUrl = ApiClient.getBaseUrl();
                fullUrl = baseUrl + avatarUrl;
            }

            Glide.with(this)
                    .load(fullUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(ivUserAvatar);
        } else {
            ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
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
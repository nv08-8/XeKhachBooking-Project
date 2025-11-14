package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail;
    private Button btnLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);

        sessionManager = new SessionManager(this);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // Set user info
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getUserEmail());

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserAccountActivity.this, GuestHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_account);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { // Sửa mục menu điều hướng
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_account) {
                // Already on the account screen, do nothing
                return true;
            } else {
                // Handle other menu items here
                return false;
            }
        });
    }
}

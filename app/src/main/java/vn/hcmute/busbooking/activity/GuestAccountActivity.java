package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;

public class GuestAccountActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_account);

        bottomNav = findViewById(R.id.bottom_navigation);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Set the selected item
        int selectedItemId = getIntent().getIntExtra("SELECTED_ITEM", R.id.nav_account);
        bottomNav.setSelectedItemId(selectedItemId);


        // Handle Bottom Navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, GuestFavoritesActivity.class));
                overridePendingTransition(0, 0);
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
}

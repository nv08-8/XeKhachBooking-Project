package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
        // Check intent to see which item to select
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
            } else if (itemId == R.id.nav_account || itemId == R.id.nav_tickets) {
                // Already on this screen, do nothing
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

package vn.hcmute.busbooking;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.hcmute.busbooking.fragment.AccountFragment;
import vn.hcmute.busbooking.fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_search) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            } else {
                // Handle other menu items here (e.g., My Tickets, Favorites)
                // For now, we can default to HomeFragment
                selectedFragment = new HomeFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });

        // Set the default fragment when the app opens
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }
    }
}

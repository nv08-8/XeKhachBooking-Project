package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        Button btnManageRoutes = findViewById(R.id.btnManageUsers); // reused id
        Button btnManageBookings = findViewById(R.id.btnManageBookings);
        Button btnRevenueStats = findViewById(R.id.btnRevenueStats);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (tvGreeting != null) tvGreeting.setText(R.string.admin_panel_title);

        if (btnManageRoutes != null) btnManageRoutes.setOnClickListener(v -> {
            startActivity(new Intent(AdminHomeActivity.this, ManageRoutesActivity.class));
        });

        if (btnManageBookings != null) btnManageBookings.setOnClickListener(v -> {
            startActivity(new Intent(AdminHomeActivity.this, ManageBookingsActivity.class));
        });

        if (btnRevenueStats != null) btnRevenueStats.setOnClickListener(v -> {
            startActivity(new Intent(AdminHomeActivity.this, RevenueStatsActivity.class));
        });

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            // Clear activity stack and go back to login
            Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

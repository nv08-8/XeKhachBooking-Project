package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        TextView tvGreeting = findViewById(R.id.tvGreeting);

        View btnManageRoutes = findViewById(R.id.btnManageRoutes);
        View btnManageBookings = findViewById(R.id.btnManageBookings);
        View btnRevenueStats = findViewById(R.id.btnRevenueStats);
        View btnManageUsers = findViewById(R.id.btnManageUsers);
        View btnLogout = findViewById(R.id.btnLogout);

        if (tvGreeting != null) tvGreeting.setText(R.string.admin_panel_title);

        if (btnManageRoutes != null) btnManageRoutes.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageRoutesActivity.class)));
        if (btnManageBookings != null) btnManageBookings.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageBookingsActivity.class)));
        if (btnRevenueStats != null) btnRevenueStats.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, RevenueStatsActivity.class)));
        if (btnManageUsers != null) btnManageUsers.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageUsersActivity.class)));

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

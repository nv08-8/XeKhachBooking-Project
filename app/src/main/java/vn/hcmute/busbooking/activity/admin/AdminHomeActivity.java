package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RevenueStatsActivity;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        TextView tvGreeting = findViewById(R.id.tvGreeting);

        View btnManageRoutes = findViewById(R.id.btnManageRoutes);
        View btnManageTrips = findViewById(R.id.btnManageTrips);
        View btnManageBookings = findViewById(R.id.btnManageBookings);
        View btnRevenueStats = findViewById(R.id.btnRevenueStats);
        View btnManageUsers = findViewById(R.id.btnManageUsers);
        View btnManageDrivers = findViewById(R.id.btnManageDrivers);
        View btnManageReviews = findViewById(R.id.btnManageReviews);
        View btnManagePromotions = findViewById(R.id.btnManagePromotions);
        View btnLogout = findViewById(R.id.btnLogout);

        if (tvGreeting != null) tvGreeting.setText(R.string.admin_panel_title);

        if (btnManageRoutes != null) btnManageRoutes.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageRoutesActivity.class)));
        if (btnManageTrips != null) btnManageTrips.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageTripsActivity.class)));
        if (btnManageBookings != null) btnManageBookings.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageBookingsActivity.class)));
        if (btnRevenueStats != null) btnRevenueStats.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, RevenueStatsActivity.class)));
        if (btnManageUsers != null) btnManageUsers.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageUsersActivity.class)));
        if (btnManageDrivers != null) btnManageDrivers.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageDriversActivity.class)));

        // TODO: Create these activities
        if (btnManageReviews != null) btnManageReviews.setOnClickListener(v -> showToast("Chức năng đang được phát triển"));
        if (btnManagePromotions != null) btnManagePromotions.setOnClickListener(v -> showToast("Chức năng đang được phát triển"));

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

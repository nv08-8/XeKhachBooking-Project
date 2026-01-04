package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RevenueStatsActivity;
import vn.hcmute.busbooking.activity.TripFeedbackListActivity;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);


        View btnManageRoutes = findViewById(R.id.btnManageRoutes);
        View btnManageTrips = findViewById(R.id.btnManageTrips);
        View btnManageBookings = findViewById(R.id.btnManageBookings);
        View btnRevenueStats = findViewById(R.id.btnRevenueStats);
        View btnManageUsers = findViewById(R.id.btnManageUsers);
        View btnManageReviews = findViewById(R.id.btnManageReviews);
        View btnManagePromotions = findViewById(R.id.btnManagePromotions);
        View btnLogout = findViewById(R.id.btnLogout);


        if (btnManageRoutes != null) btnManageRoutes.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageRoutesActivity.class)));
        if (btnManageTrips != null) btnManageTrips.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageTripsActivity.class)));
        if (btnManageBookings != null) btnManageBookings.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageBookingsActivity.class)));
        if (btnRevenueStats != null) btnRevenueStats.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, RevenueStatsActivity.class)));
        if (btnManageUsers != null) btnManageUsers.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManageUsersActivity.class)));
        if (btnManageReviews != null) btnManageReviews.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, TripFeedbackListActivity.class)));
        if (btnManagePromotions != null) btnManagePromotions.setOnClickListener(v -> startActivity(new Intent(AdminHomeActivity.this, ManagePromotionsActivity.class)));

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

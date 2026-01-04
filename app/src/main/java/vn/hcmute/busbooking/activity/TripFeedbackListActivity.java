package vn.hcmute.busbooking.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class TripFeedbackListActivity extends AppCompatActivity implements
        TripFeedbackListFragment.OnTripSelectedListener,
        TripFeedbackDetailsFragment.OnBackPressedListener {

    private TripFeedbackListFragment tripListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_feedback_list_container);

        // Setup back button handler
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Hiển thị danh sách chuyến ban đầu
        if (savedInstanceState == null) {
            tripListFragment = new TripFeedbackListFragment();
            tripListFragment.setOnTripSelectedListener(this);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, tripListFragment)
                    .commit();
        }
    }

    private final androidx.activity.OnBackPressedCallback callback = new androidx.activity.OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // Quay lại danh sách chuyến hoặc đóng activity
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // Có fragment trong backstack, pop nó
                getSupportFragmentManager().popBackStack();
            } else {
                // Không có fragment trong backstack, đóng activity
                finish();
            }
        }
    };

    @Override
    public void onTripSelected(int tripId) {
        // Khi chọn chuyến, hiển thị chi tiết feedback
        TripFeedbackDetailsFragment tripDetailsFragment = TripFeedbackDetailsFragment.newInstance(tripId);
        tripDetailsFragment.setBackPressedListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, tripDetailsFragment)
                .addToBackStack(null)
                .commit();
    }
}


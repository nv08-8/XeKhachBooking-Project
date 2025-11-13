package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Trip;

public class SeatSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        Trip trip = (Trip) getIntent().getSerializableExtra("trip");

        if (trip != null) {
            // TODO: Use the trip object to display seat selection UI
        }
    }
}

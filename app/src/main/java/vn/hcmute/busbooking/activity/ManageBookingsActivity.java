package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class ManageBookingsActivity extends AppCompatActivity {

    private ListView lvBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bookings);

        lvBookings = findViewById(R.id.lvBookings);

        String[] sample = new String[]{"#BK001 - Nguyễn A - Hà Nội -> Hải Phòng", "#BK002 - Trần B - Hà Nội -> Nam Định"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sample);
        lvBookings.setAdapter(adapter);

        lvBookings.setOnItemClickListener((parent, view, position, id) -> {
            // open detail
            Intent intent = new Intent(ManageBookingsActivity.this, BookingAdminDetailActivity.class);
            // intent.putExtra("booking_id", ...);
            startActivity(intent);
        });
    }
}


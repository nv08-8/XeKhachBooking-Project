package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class BookingAdminDetailActivity extends AppCompatActivity {

    private TextView tvBookingInfo;
    private Button btnConfirm, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_admin_detail);

        tvBookingInfo = findViewById(R.id.tvBookingInfo);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        btnCancel = findViewById(R.id.btnCancelBooking);

        tvBookingInfo.setText("Chi tiết booking (mock)");

        btnConfirm.setOnClickListener(v -> Toast.makeText(BookingAdminDetailActivity.this, "Xác nhận (chưa triển khai)", Toast.LENGTH_SHORT).show());
        btnCancel.setOnClickListener(v -> Toast.makeText(BookingAdminDetailActivity.this, "Hủy (chưa triển khai)", Toast.LENGTH_SHORT).show());
    }
}


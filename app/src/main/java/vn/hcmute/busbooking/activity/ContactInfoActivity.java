package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class ContactInfoActivity extends AppCompatActivity {

    private Trip trip;
    private ArrayList<String> seatLabels;
    private Location selectedPickup;
    private Location selectedDropoff;

    private TextInputEditText etFullName;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etEmail;
    private TextView tvSubtotal;
    private Button btnContinue;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ContactInfo", "onCreate started");

        setContentView(R.layout.activity_contact_info);
        android.util.Log.d("ContactInfo", "Layout set");

        // Get data from previous activity
        trip = getIntent().getParcelableExtra("trip");
        seatLabels = getIntent().getStringArrayListExtra("seat_labels");
        selectedPickup = getIntent().getParcelableExtra("pickup_location");
        selectedDropoff = getIntent().getParcelableExtra("dropoff_location");

        android.util.Log.d("ContactInfo", "Trip: " + (trip != null ? trip.getId() : "null"));
        android.util.Log.d("ContactInfo", "Seats: " + (seatLabels != null ? seatLabels.size() : "null"));
        android.util.Log.d("ContactInfo", "Pickup: " + (selectedPickup != null ? selectedPickup.getName() : "null"));
        android.util.Log.d("ContactInfo", "Dropoff: " + (selectedDropoff != null ? selectedDropoff.getName() : "null"));

        if (trip == null || seatLabels == null || seatLabels.isEmpty() ||
            selectedPickup == null || selectedDropoff == null) {
            android.util.Log.e("ContactInfo", "Missing required data - finishing activity");
            Toast.makeText(this, "Lỗi: Thiếu thông tin đặt vé.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        android.util.Log.d("ContactInfo", "SessionManager initialized, logged in: " + sessionManager.isLoggedIn());

        // Initialize views
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        btnContinue = findViewById(R.id.btnContinue);

        // Setup toolbar
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // Pre-fill with user info if logged in (user can still edit)
        if (sessionManager.isLoggedIn()) {
            String userName = sessionManager.getUserName();
            String userPhone = sessionManager.getUserPhone();
            String userEmail = sessionManager.getUserEmail();

            if (userName != null && !userName.isEmpty()) {
                etFullName.setText(userName);
            }
            if (userPhone != null && !userPhone.isEmpty()) {
                etPhoneNumber.setText(userPhone);
            }
            if (userEmail != null && !userEmail.isEmpty()) {
                etEmail.setText(userEmail);
            }
        }

        // Display subtotal
        if (seatLabels != null && trip != null) {
            double totalAmount = seatLabels.size() * trip.getPrice();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvSubtotal.setText(formatter.format(totalAmount));
        }

        // Handle continue button
        btnContinue.setOnClickListener(v -> validateAndContinue());
    }

    private void validateAndContinue() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String phoneNumber = etPhoneNumber.getText() != null ? etPhoneNumber.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ và tên");
            etFullName.requestFocus();
            return;
        }

        // Validate phone number
        if (TextUtils.isEmpty(phoneNumber)) {
            etPhoneNumber.setError("Vui lòng nhập số điện thoại");
            etPhoneNumber.requestFocus();
            return;
        }

        if (phoneNumber.length() < 10) {
            etPhoneNumber.setError("Số điện thoại không hợp lệ");
            etPhoneNumber.requestFocus();
            return;
        }

        // Validate email if provided
        if (!TextUtils.isEmpty(email)) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Email không hợp lệ");
                etEmail.requestFocus();
                return;
            }
        }

        // Go to payment activity
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("trip", trip);
        intent.putStringArrayListExtra("seat_labels", seatLabels);
        intent.putExtra("pickup_stop_id", selectedPickup.getId());
        intent.putExtra("pickup_stop_name", formatLocationName(selectedPickup));
        intent.putExtra("dropoff_stop_id", selectedDropoff.getId());
        intent.putExtra("dropoff_stop_name", formatLocationName(selectedDropoff));
        intent.putExtra("passenger_name", fullName);
        intent.putExtra("passenger_phone", phoneNumber);
        intent.putExtra("passenger_email", email);
        startActivity(intent);
    }

    private String formatLocationName(Location location) {
        String name = location.getName();
        String address = location.getAddress();

        if (name != null && !name.isEmpty() && address != null && !address.isEmpty()) {
            return name + " - " + address;
        } else if (name != null && !name.isEmpty()) {
            return name;
        } else if (address != null && !address.isEmpty()) {
            return address;
        } else {
            return "Điểm dừng không tên";
        }
    }
}


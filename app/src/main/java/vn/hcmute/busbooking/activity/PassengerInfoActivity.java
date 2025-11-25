package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class PassengerInfoActivity extends AppCompatActivity {

    private EditText etFullName, etPhoneNumber, etEmail;
    private Button btnContinue;

    // Data from previous activity
    private int tripId;
    private ArrayList<String> seatLabels;
    private int amount;
    private String origin, destination, operator;
    private boolean isReturn;
    private String returnOrigin, returnDestination, returnDate;
    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_info);

        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Bind views
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        btnContinue = findViewById(R.id.btnContinue);

        // Pre-fill with user info
        etFullName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());
        etPhoneNumber.setText(sessionManager.getUserPhone());

        // Get data from Intent
        Intent intent = getIntent();
        tripId = intent.getIntExtra("trip_id", 0);
        seatLabels = intent.getStringArrayListExtra("seat_labels");
        amount = intent.getIntExtra("amount", 0);
        origin = intent.getStringExtra("origin");
        destination = intent.getStringExtra("destination");
        operator = intent.getStringExtra("operator");
        isReturn = intent.getBooleanExtra("isReturn", false);
        returnOrigin = intent.getStringExtra("returnOrigin");
        returnDestination = intent.getStringExtra("returnDestination");
        returnDate = intent.getStringExtra("returnDate");

        btnContinue.setOnClickListener(v -> {
            if (validateInput()) {
                navigateToPayment();
            }
        });
    }

    private boolean validateInput() {
        String fullName = etFullName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ và tên");
            etFullName.requestFocus();
            return false;
        }

        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError("Vui lòng nhập số điện thoại");
            etPhoneNumber.requestFocus();
            return false;
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            etPhoneNumber.setError("Số điện thoại không hợp lệ");
            etPhoneNumber.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidPhoneNumber(String phone) {
        // Must start with 0 and have 10 digits total
        return phone.matches("0[0-9]{9}");
    }

    private void navigateToPayment() {
        Intent paymentIntent = new Intent(this, PaymentActivity.class);

        // Pass all the data forward
        paymentIntent.putExtra("trip_id", tripId);
        paymentIntent.putStringArrayListExtra("seat_labels", seatLabels);
        paymentIntent.putExtra("amount", amount);
        paymentIntent.putExtra("origin", origin);
        paymentIntent.putExtra("destination", destination);
        paymentIntent.putExtra("operator", operator);
        paymentIntent.putExtra("isReturn", isReturn);
        paymentIntent.putExtra("returnOrigin", returnOrigin);
        paymentIntent.putExtra("returnDestination", returnDestination);
        paymentIntent.putExtra("returnDate", returnDate);

        // Add passenger info
        paymentIntent.putExtra("fullName", etFullName.getText().toString());
        paymentIntent.putExtra("phoneNumber", etPhoneNumber.getText().toString());
        paymentIntent.putExtra("email", etEmail.getText().toString());

        startActivity(paymentIntent);
    }
}

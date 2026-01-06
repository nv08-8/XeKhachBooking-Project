package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.CurrencyUtil;
import vn.hcmute.busbooking.utils.SessionManager;

public class ContactInfoActivity extends AppCompatActivity {

    private Trip trip;
    private ArrayList<String> seatLabels;
    private Location selectedPickup;
    private Location selectedDropoff;

    // New: return leg fields
    private Trip returnTrip;
    private ArrayList<String> returnSeatLabels;
    private Location returnSelectedPickup;
    private Location returnSelectedDropoff;

    // Support explicit depart_* carry-through
    private Trip departTripExplicit;
    private ArrayList<String> departSeatLabelsExplicit;
    private Location departSelectedPickupExplicit;
    private Location departSelectedDropoffExplicit;

    private TextInputEditText etFullName;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etEmail;
    private TextView tvSubtotal;
    private Button btnContinue;

    private TextView tvDepartSummary, tvReturnSummary;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ContactInfo", "onCreate started");

        setContentView(R.layout.activity_contact_info);
        android.util.Log.d("ContactInfo", "Layout set");

        // Get data from previous activity (may be depart or return selection flows)
        Intent src = getIntent();
        trip = src.getParcelableExtra("trip");
        seatLabels = src.getStringArrayListExtra("seat_labels");
        selectedPickup = src.getParcelableExtra("pickup_location");
        selectedDropoff = src.getParcelableExtra("dropoff_location");

        // Read explicit depart_* if present (these are carried when switching to return search)
        departTripExplicit = src.getParcelableExtra("depart_trip");
        departSeatLabelsExplicit = src.getStringArrayListExtra("depart_seat_labels");
        departSelectedPickupExplicit = src.getParcelableExtra("depart_pickup_location");
        departSelectedDropoffExplicit = src.getParcelableExtra("depart_dropoff_location");

        // Also read possible return leg extras
        returnTrip = src.getParcelableExtra("return_trip");
        returnSeatLabels = src.getStringArrayListExtra("return_seat_labels");
        returnSelectedPickup = src.getParcelableExtra("return_pickup_location");
        returnSelectedDropoff = src.getParcelableExtra("return_dropoff_location");

        android.util.Log.d("ContactInfo", "Trip: " + (trip != null ? trip.getId() : "null"));
        android.util.Log.d("ContactInfo", "Seats: " + (seatLabels != null ? seatLabels.size() : "null"));
        android.util.Log.d("ContactInfo", "Pickup: " + (selectedPickup != null ? selectedPickup.getName() : "null"));
        android.util.Log.d("ContactInfo", "Dropoff: " + (selectedDropoff != null ? selectedDropoff.getName() : "null"));
        android.util.Log.d("ContactInfo", "ReturnTrip: " + (returnTrip != null ? returnTrip.getId() : "null"));
        android.util.Log.d("ContactInfo", "DepartExplicitTrip: " + (departTripExplicit != null ? departTripExplicit.getId() : "null"));

        // Determine explicit leg objects to display (avoid overwriting primary with return values)
        // Build depart leg from explicit depart_* if available, otherwise from primary trip/pickup/dropoff
        Trip legDepartTrip = departTripExplicit != null ? departTripExplicit : trip;
        ArrayList<String> legDepartSeats = departSeatLabelsExplicit != null ? departSeatLabelsExplicit : seatLabels;
        Location legDepartPickup = departSelectedPickupExplicit != null ? departSelectedPickupExplicit : selectedPickup;
        Location legDepartDropoff = departSelectedDropoffExplicit != null ? departSelectedDropoffExplicit : selectedDropoff;

        // Build return leg from explicit return_* if available
        Trip legReturnTrip = returnTrip;
        ArrayList<String> legReturnSeats = returnSeatLabels;
        Location legReturnPickup = returnSelectedPickup;
        Location legReturnDropoff = returnSelectedDropoff;

        // If depart explicit was provided, and original primary trip info referred to what is actually the return leg,
        // ensure primary variables still reflect depart leg for subsequent code paths that expect 'trip' to be depart.
        if (departTripExplicit != null) {
            // Keep original 'trip' as departTripExplicit so downstream logic that uses 'trip'/'seatLabels' refers to depart
            trip = legDepartTrip;
            seatLabels = legDepartSeats;
            selectedPickup = legDepartPickup;
            selectedDropoff = legDepartDropoff;
        }

        // Initialize summaries views after resolving leg variables
        sessionManager = new SessionManager(this);
        android.util.Log.d("ContactInfo", "SessionManager initialized, logged in: " + sessionManager.isLoggedIn());

        // Initialize views
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        btnContinue = findViewById(R.id.btnContinue);

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

        // Read depart summary views
        tvDepartSummary = findViewById(R.id.tvDepartSummary);
        tvReturnSummary = findViewById(R.id.tvReturnSummary);

        // Display subtotal (include return leg if present)
        double total = 0.0;
        if (legDepartSeats != null && legDepartTrip != null) {
            total += legDepartSeats.size() * legDepartTrip.getPrice();
        }
        if (legReturnSeats != null && legReturnTrip != null) {
            total += legReturnSeats.size() * legReturnTrip.getPrice();
        }
        tvSubtotal.setText(CurrencyUtil.formatVND(total));

        // Setup toolbar
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // Show depart and return summaries if returnTrip provided
        if (tvDepartSummary != null) {
            tvDepartSummary.setText(buildLegSummary(legDepartTrip, legDepartSeats, legDepartPickup, legDepartDropoff));
        }
        if (legReturnTrip != null && tvReturnSummary != null) {
            tvReturnSummary.setText(buildLegSummary(legReturnTrip, legReturnSeats, legReturnPickup, legReturnDropoff));
            tvReturnSummary.setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.summaryContainer).setVisibility(android.view.View.VISIBLE);
        } else {
            // If no return trip, still show depart summary container for clarity
            if (tvDepartSummary != null) {
                findViewById(R.id.summaryContainer).setVisibility(android.view.View.VISIBLE);
            }
        }

        // Handle continue button
        btnContinue.setOnClickListener(v -> validateAndContinue());
    }

    private String buildLegSummary(Trip t, ArrayList<String> seats, Location pu, Location dof) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        String origin = t.getOrigin() != null ? t.getOrigin() : "";
        String destination = t.getDestination() != null ? t.getDestination() : "";
        sb.append("").append(origin).append(" → ").append(destination);
        if (t.getDepartureTime() != null) {
            // show date part only
            String dt = t.getDepartureTime();
            // try to extract date if ISO format
            if (dt.length() >= 10) dt = dt.substring(0, 10);
            sb.append(" | ").append(dt);
        }
        if (seats != null && !seats.isEmpty()) {
            sb.append(" | Ghế: ").append(TextUtils.join(", ", seats));
        }
        if (pu != null) sb.append(" | Đón: ").append(pu.getName());
        if (dof != null) sb.append(" | Trả: ").append(dof.getName());
        return sb.toString();
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

        // Determine final depart leg values (prefer explicit depart_* if present)
        Trip departTrip = departTripExplicit != null ? departTripExplicit : trip;
        ArrayList<String> departSeats = departSeatLabelsExplicit != null ? departSeatLabelsExplicit : seatLabels;
        Location departPickup = departSelectedPickupExplicit != null ? departSelectedPickupExplicit : selectedPickup;
        Location departDropoff = departSelectedDropoffExplicit != null ? departSelectedDropoffExplicit : selectedDropoff;

        // Basic validation again
        if (departTrip == null || departSeats == null || departSeats.isEmpty() || departPickup == null || departDropoff == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin chiều đi.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Go to payment activity
        // If this is a round-trip booking but return leg data is missing, redirect user to choose return trip
        Intent src = getIntent();
        boolean isRoundTrip = src.getBooleanExtra("isRoundTrip", false);
        if (isRoundTrip && (returnTrip == null || returnSeatLabels == null || returnSeatLabels.isEmpty() || returnSelectedPickup == null || returnSelectedDropoff == null)) {
            // Need to choose return trip first - forward depart_* info and mark round_trip_phase
            Intent toReturnSearch = new Intent(this, TripListActivity.class);
            // Determine the origin/destination for return search (swap)
            String returnDate = src.getStringExtra("returnDate");
            String returnOrigin = src.getStringExtra("returnOrigin");
            String returnDestination = src.getStringExtra("returnDestination");
            if (returnOrigin == null || returnOrigin.isEmpty()) returnOrigin = trip != null ? trip.getDestination() : null;
            if (returnDestination == null || returnDestination.isEmpty()) returnDestination = trip != null ? trip.getOrigin() : null;
            if (returnOrigin != null) toReturnSearch.putExtra("origin", returnOrigin);
            if (returnDestination != null) toReturnSearch.putExtra("destination", returnDestination);
            if (returnDate != null) toReturnSearch.putExtra("date", returnDate);
            // mark phase and carry depart_* keys
            toReturnSearch.putExtra("round_trip_phase", true);
            toReturnSearch.putExtra("isRoundTrip", true);
            toReturnSearch.putExtra("depart_trip", departTripExplicit != null ? departTripExplicit : trip);
            toReturnSearch.putStringArrayListExtra("depart_seat_labels", departSeatLabelsExplicit != null ? departSeatLabelsExplicit : seatLabels);
            toReturnSearch.putExtra("depart_pickup_location", departSelectedPickupExplicit != null ? departSelectedPickupExplicit : selectedPickup);
            toReturnSearch.putExtra("depart_dropoff_location", departSelectedDropoffExplicit != null ? departSelectedDropoffExplicit : selectedDropoff);
            startActivity(toReturnSearch);
            return;
        }

        Intent intent = new Intent(this, PaymentActivity.class);
         // Primary (depart) leg
         intent.putExtra("trip", departTrip);
         intent.putStringArrayListExtra("seat_labels", departSeats);
         intent.putExtra("pickup_stop_id", departPickup.getId());
         intent.putExtra("pickup_stop_name", formatLocationName(departPickup));
         intent.putExtra("dropoff_stop_id", departDropoff.getId());
         intent.putExtra("dropoff_stop_name", formatLocationName(departDropoff));
         intent.putExtra("passenger_name", fullName);
         intent.putExtra("passenger_phone", phoneNumber);
         intent.putExtra("passenger_email", email);

         // If return leg provided, attach return_* extras
         if (returnTrip != null && returnSeatLabels != null && !returnSeatLabels.isEmpty() && returnSelectedPickup != null && returnSelectedDropoff != null) {
             intent.putExtra("return_trip", returnTrip);
             intent.putStringArrayListExtra("return_seat_labels", returnSeatLabels);
             intent.putExtra("return_pickup_stop_id", returnSelectedPickup.getId());
             intent.putExtra("return_pickup_stop_name", formatLocationName(returnSelectedPickup));
             intent.putExtra("return_dropoff_stop_id", returnSelectedDropoff.getId());
             intent.putExtra("return_dropoff_stop_name", formatLocationName(returnSelectedDropoff));
             intent.putExtra("isRoundTrip", true);
         }

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

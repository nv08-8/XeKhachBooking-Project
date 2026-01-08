package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PaymentResponse;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.utils.CurrencyUtil;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String PREFS_NAME = "payment_timers";
    private static final long HOLD_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    private TextView tvBusOperator, tvBusType, tvAppName, tvPickup, tvDropoff, tvSeat, tvDepartureTime;
    private TextView tvOrigin, tvDestination;
    private TextView tvCountdown;
    private TextView tvPassengerName, tvPassengerPhone;
    // Pickup / Dropoff time views (present in layout)
    private TextView tvPickupTimeView;
    private TextView tvDropoffTimeView;
    // NEW: separate views for return leg times
    private TextView tvReturnPickupTimeView;
    private TextView tvReturnDropoffTimeView;
    private TextView tvBottomTotal;
    private Button btnConfirmPayment;
    private com.google.android.material.button.MaterialButton btnChangeToOffline;
    private ProgressBar progressBar;

    private MaterialCardView cardCreditCard, cardQrPayment, cardPayAtOffice;
    private RadioButton rbCreditCard, rbQrPayment, rbPayAtOffice;
    private LinearLayout creditCardForm;
    // Card input fields
    private EditText etCardNumber;
    private EditText etExpiryDate;
    private EditText etCvv;

    private Trip trip;
    private ArrayList<String> seatLabels;
    // return leg
    private Trip returnTrip = null;
    private ArrayList<String> returnSeatLabels = null;
    private int returnPickupStopId = -1;
    private int returnDropoffStopId = -1;
    private String returnPickupStopName = null;
    private String returnDropoffStopName = null;
    private ApiService apiService;
    private SessionManager sessionManager;
    private Double bookingTotalAmount = null; // if opening for existing booking, use this amount when available

    private boolean isPendingPayment;
    private ArrayList<Integer> bookingIds;
    private String fullName, phoneNumber, email;

    // Track current payment method to prevent auto-cancel for offline payments
    private String currentPaymentMethod = null;
    private boolean isOfflinePayment = false;

    private int pickupStopId;
    private int dropoffStopId;
    private String pickupStopName;
    private String dropoffStopName;

    private android.os.CountDownTimer countDownTimer;
    private MaterialCardView cardCountdown;

    // Promo fields
    private EditText etPromoCode;
    private com.google.android.material.button.MaterialButton btnApplyPromo;
    private TextView tvPromoDetails, tvDiscountApplied, tvSubtotal, tvTotal, tvSelectPromo;
    private String appliedPromotionCode = null;
    private double appliedDiscount = 0.0;
    private Map<String, Object> appliedPromotion = null;
    private long lastApplyClickMs = 0L;

    // Coin fields
    private TextView tvUserCoinBalance;
    private com.google.android.material.switchmaterial.SwitchMaterial switchUseCoins;
    private LinearLayout layoutUseCoins;
    private int userCoinBalance = 0;
    private int usedCoinAmount = 0; // Track how many coins are being used

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_payment);

        View mainView = findViewById(R.id.main);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            appBarLayout.setPadding(0, topInset, 0, 0);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        initializeViews();

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        if (!collectIntentData()) {
            Toast.makeText(this, getString(R.string.msg_booking_error, "Dữ liệu đặt vé không hợp lệ."), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Backwards compatibility: if caller provided a single booking_id (legacy), treat it as pending-payment flow
        try {
            if (!isPendingPayment) {
                int singleBookingId = getIntent().getIntExtra("booking_id", -1);
                if (singleBookingId > 0) {
                    isPendingPayment = true;
                    bookingIds = new ArrayList<>();
                    bookingIds.add(singleBookingId);
                    Toast.makeText(this, "Mở trang thanh toán cho vé #" + singleBookingId, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}

        // If this is a pending-payment flow and we only received booking_ids, fetch booking details first
        if (isPendingPayment && (trip == null || seatLabels == null || seatLabels.isEmpty())) {
            // fetch booking details for primary booking id
            if (bookingIds != null && !bookingIds.isEmpty()) {
                fetchBookingDetailsAndInit(bookingIds.get(0));
            } else {
                Toast.makeText(this, getString(R.string.msg_booking_error, "Thiếu mã đặt vé để thanh toán."), Toast.LENGTH_SHORT).show();
                finish();
            }
            // return here because initialization will continue in callback
            return;
        } else {
            populateBookingSummary();
            setupPaymentMethodSelection();
            setupPromoHandlers(); // Setup promotion handlers
            loadUserCoinBalance(); // Load user's coin balance

            if (isPendingPayment) {
                // Check current payment method from booking details
                // This will be populated from fetchBookingDetailsAndInit callback
                // For now, we'll determine if it's online or offline payment
                // Default to QR for backwards compatibility, but allow switching
                selectPaymentMethod(R.id.rbQrPayment);

                // Do NOT disable other payment methods - allow user to switch
                // The UI will show all options so user can change payment method

                // Only show countdown and start timer if this is truly an online payment
                // We'll check this after fetching booking details
            }
        }

        btnChangeToOffline.setOnClickListener(v -> changePaymentMethodToOffline());

        btnConfirmPayment.setOnClickListener(v -> {
            Log.d(TAG, "🔵 btnConfirmPayment CLICKED!");
            Log.d(TAG, "isPendingPayment=" + isPendingPayment + ", bookingIds=" + (bookingIds != null ? bookingIds : "null"));

            if (isPendingPayment) {
                // Check if user is trying to change payment method
                int selectedMethod = getSelectedPaymentMethodId();
                String newPaymentMethod = null;

                if (selectedMethod == R.id.rbQrPayment) {
                    newPaymentMethod = "qr";
                } else if (selectedMethod == R.id.rbCreditCard) {
                    newPaymentMethod = "card";
                } else if (selectedMethod == R.id.rbPayAtOffice) {
                    newPaymentMethod = "cash";
                }

                Log.d(TAG, "btnConfirmPayment clicked: selectedMethod=" + selectedMethod +
                           ", newPaymentMethod=" + newPaymentMethod +
                           ", currentPaymentMethod=" + currentPaymentMethod +
                           ", bookingIds=" + (bookingIds != null ? bookingIds.size() : "null"));

                // ✅ Normalize payment method mapping for comparison
                String normalizedNew = normalizePaymentMethod(newPaymentMethod);
                String normalizedCurrent = normalizePaymentMethod(currentPaymentMethod);

                // Check if payment method actually changed
                boolean paymentMethodChanged = !normalizedNew.equals(normalizedCurrent);

                Log.d(TAG, "Payment method check: current=" + normalizedCurrent +
                           ", new=" + normalizedNew + ", changed=" + paymentMethodChanged);

                // If changing to QR, process payment
                if (selectedMethod == R.id.rbQrPayment) {
                    Log.d(TAG, "User wants QR payment. paymentMethodChanged=" + paymentMethodChanged);
                    // Calculate used coins before processing
                    usedCoinAmount = calculateUsedCoins();
                    Log.d(TAG, "🪙 After calculateUsedCoins for QR: usedCoinAmount=" + usedCoinAmount);
                    if (paymentMethodChanged && bookingIds != null && !bookingIds.isEmpty()) {
                        // Need to change payment method first
                        Log.d(TAG, "Calling changePaymentMethodForBookings to QR");
                        setLoadingState(true);
                        changePaymentMethodForBookings(0, newPaymentMethod, () -> {
                            // After successfully changing to QR, process payment
                            Log.d(TAG, "Payment method changed to QR, now processing payment");
                            processPayosPayment(bookingIds);
                        });
                    } else {
                        // Payment method is already QR, process directly
                        Log.d(TAG, "Payment method already QR, processing payment directly");
                        processPayosPayment(bookingIds);
                    }
                } else if (selectedMethod == R.id.rbCreditCard) {
                    Log.d(TAG, "User wants Card payment. paymentMethodChanged=" + paymentMethodChanged);
                    // Calculate used coins before processing
                    usedCoinAmount = calculateUsedCoins();
                    Log.d(TAG, "🪙 After calculateUsedCoins for Card: usedCoinAmount=" + usedCoinAmount);
                    // Validate card inputs before proceeding
                    if (!validateCardInputs()) {
                        // validateCardInputs shows appropriate messages
                        return;
                    }
                    if (paymentMethodChanged && bookingIds != null && !bookingIds.isEmpty()) {
                        // Need to change payment method first
                        Log.d(TAG, "Calling changePaymentMethodForBookings to Card");
                        setLoadingState(true);
                        changePaymentMethodForBookings(0, newPaymentMethod, () -> {
                            // After changing to card, confirm payment
                            Log.d(TAG, "Payment method changed to Card, now confirming payment");
                            confirmNextPayment(bookingIds, 0, "card");
                        });
                    } else {
                        // Payment method is already Card, process directly
                        Log.d(TAG, "Payment method already Card, processing payment directly");
                        confirmNextPayment(bookingIds, 0, "card");
                    }
                } else if (selectedMethod == R.id.rbPayAtOffice) {
                    Log.d(TAG, "User wants Cash payment. paymentMethodChanged=" + paymentMethodChanged);
                    // Calculate used coins before processing
                    usedCoinAmount = calculateUsedCoins();
                    Log.d(TAG, "🪙 After calculateUsedCoins for Cash: usedCoinAmount=" + usedCoinAmount);
                    if (paymentMethodChanged) {
                        // Change to offline payment
                        changePaymentMethodToOfflineConfirm(newPaymentMethod);
                    } else {
                        // Already offline, just show message
                        Log.d(TAG, "Payment method already Cash, showing info");
                        Toast.makeText(PaymentActivity.this,
                                "Vé đã được đặt với thanh toán tại nhà xe. Vui lòng thanh toán trước khi lên xe.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Calculate used coins before processing payment
                usedCoinAmount = calculateUsedCoins();
                Log.d(TAG, "🪙 After calculateUsedCoins for new booking: usedCoinAmount=" + usedCoinAmount);
                createBookingAndProcessPayment();
            }
        });
    }

    private void initializeViews() {
        tvBusOperator = findViewById(R.id.tvBusOperator);
        tvBusType = findViewById(R.id.tvBusType);
        tvAppName = findViewById(R.id.tvAppName);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDestination = findViewById(R.id.tvDestination);
        // Pickup / Dropoff time views (present in layout)
        try { tvPickupTimeView = findViewById(R.id.tvPickupTime); } catch (Exception ignored) {}
        try { tvDropoffTimeView = findViewById(R.id.tvDropoffTime); } catch (Exception ignored) {}

        // NEW: bind return-leg specific time views
        try { tvReturnPickupTimeView = findViewById(R.id.tvReturnPickupTime); } catch (Exception ignored) {}
        try { tvReturnDropoffTimeView = findViewById(R.id.tvReturnDropoffTime); } catch (Exception ignored) {}

        // Layout uses tvPickupLocation / tvDropoffLocation ids — map them to tvPickup/tvDropoff variables
        tvPickup = findViewById(R.id.tvPickupLocation);
        tvDropoff = findViewById(R.id.tvDropoffLocation);
        // New: find separate address TextViews in layout
        TextView tvPickupAddressView = null;
        TextView tvDropoffAddressView = null;
        try {
            tvPickupAddressView = findViewById(R.id.tvPickupAddress);
        } catch (Exception ignored) {}
        try {
            tvDropoffAddressView = findViewById(R.id.tvDropoffAddress);
        } catch (Exception ignored) {}
        // store into member vars by reusing tvPickup/tvDropoff references for names and temporary locals for addresses
        // We'll use tags on tvPickup/tvDropoff to keep address views accessible in other methods
        if (tvPickup != null && tvPickupAddressView != null) tvPickup.setTag(tvPickupAddressView);
        if (tvDropoff != null && tvDropoffAddressView != null) tvDropoff.setTag(tvDropoffAddressView);
        // tvDate view doesn't exist in layout; we don't use it, avoid null references
        // tvDate = null; // findViewById(R.id.tvDate);
        // seat textview id in layout is tvSeatNumber
        tvSeat = findViewById(R.id.tvSeatNumber);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        // phone field id in layout is tvPhoneNumber
        tvPassengerPhone = findViewById(R.id.tvPhoneNumber);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        // tvArrivalTime view doesn't exist in layout; we don't use it
        // tvArrivalTime = null; // findViewById(R.id.tvArrivalTime);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvBottomTotal = findViewById(R.id.tvBottomTotal);
        cardCountdown = findViewById(R.id.cardCountdown);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        btnChangeToOffline = findViewById(R.id.btnChangeToOffline);
        progressBar = new ProgressBar(this);
        cardCreditCard = findViewById(R.id.cardCreditCard);
        cardQrPayment = findViewById(R.id.cardQrPayment);
        cardPayAtOffice = findViewById(R.id.cardPayAtOffice);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        rbQrPayment = findViewById(R.id.rbQrPayment);
        rbPayAtOffice = findViewById(R.id.rbPayAtOffice);
        creditCardForm = findViewById(R.id.creditCardForm);

        // Bind card input fields
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        etCvv = findViewById(R.id.etCvv);
        // Prevent IME 'Next' or 'Done' from auto-submitting/moving focus while typing card details
        try {
            if (etCardNumber != null) etCardNumber.setImeOptions(EditorInfo.IME_ACTION_NONE);
            if (etExpiryDate != null) etExpiryDate.setImeOptions(EditorInfo.IME_ACTION_NONE);
            if (etCvv != null) etCvv.setImeOptions(EditorInfo.IME_ACTION_NONE);
        } catch (Exception ignored) {}

        // Promo-related views
        etPromoCode = findViewById(R.id.etPromoCode);
        btnApplyPromo = findViewById(R.id.btnApplyPromo);
        tvPromoDetails = findViewById(R.id.tvPromoDetails);
        tvDiscountApplied = findViewById(R.id.tvDiscountApplied);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        tvSelectPromo = findViewById(R.id.tvSelectPromo);

        // Coin-related views
        tvUserCoinBalance = findViewById(R.id.tvUserCoinBalance);
        switchUseCoins = findViewById(R.id.switchUseCoins);
        layoutUseCoins = findViewById(R.id.layoutUseCoins);
    }

    private boolean collectIntentData() {
         Intent intent = getIntent();
         isPendingPayment = intent.getBooleanExtra("is_pending_payment", false);

         if (isPendingPayment) {
             bookingIds = intent.getIntegerArrayListExtra("booking_ids");
         }

         trip = intent.getParcelableExtra("trip");
         seatLabels = intent.getStringArrayListExtra("seat_labels");

         // Read primary pickup/dropoff values from Intent (try ids/names and parcelable fallbacks)
         try {
             pickupStopId = intent.getIntExtra("pickup_stop_id", -1);
             dropoffStopId = intent.getIntExtra("dropoff_stop_id", -1);
             pickupStopName = intent.getStringExtra("pickup_stop_name");
             dropoffStopName = intent.getStringExtra("dropoff_stop_name");

             // Fallback: caller may have provided parcelable Location objects instead
             if ((pickupStopId == -1 || pickupStopName == null) && intent.hasExtra("pickup_location")) {
                 try {
                     vn.hcmute.busbooking.model.Location loc = intent.getParcelableExtra("pickup_location");
                     if (loc != null) {
                         if (pickupStopId == -1) pickupStopId = loc.getId();
                         if (pickupStopName == null || pickupStopName.isEmpty()) pickupStopName = (loc.getName() != null ? loc.getName() : loc.getAddress());
                     }
                 } catch (Exception ignored) {}
             }

             if ((dropoffStopId == -1 || dropoffStopName == null) && intent.hasExtra("dropoff_location")) {
                 try {
                     vn.hcmute.busbooking.model.Location loc = intent.getParcelableExtra("dropoff_location");
                     if (loc != null) {
                         if (dropoffStopId == -1) dropoffStopId = loc.getId();
                         if (dropoffStopName == null || dropoffStopName.isEmpty()) dropoffStopName = (loc.getName() != null ? loc.getName() : loc.getAddress());
                     }
                 } catch (Exception ignored) {}
             }

             Log.d(TAG, "collectIntentData: primary trip=" + (trip != null ? trip.getId() : "null") +
                     ", pickupStopId=" + pickupStopId + ", dropoffStopId=" + dropoffStopId +
                     ", pickupStopName=" + (pickupStopName != null ? pickupStopName : "null") +
                     ", dropoffStopName=" + (dropoffStopName != null ? dropoffStopName : "null"));
         } catch (Exception ignored) {}

         // Read return leg extras if present (ids/names and parcelable fallbacks)
         try {
             returnTrip = intent.getParcelableExtra("return_trip");
             returnSeatLabels = intent.getStringArrayListExtra("return_seat_labels");
             returnPickupStopId = intent.getIntExtra("return_pickup_stop_id", -1);
             returnDropoffStopId = intent.getIntExtra("return_dropoff_stop_id", -1);
             returnPickupStopName = intent.getStringExtra("return_pickup_stop_name");
             returnDropoffStopName = intent.getStringExtra("return_dropoff_stop_name");

             if ((returnPickupStopId == -1 || returnPickupStopName == null) && intent.hasExtra("return_pickup_location")) {
                 try {
                     vn.hcmute.busbooking.model.Location loc = intent.getParcelableExtra("return_pickup_location");
                     if (loc != null) {
                         if (returnPickupStopId == -1) returnPickupStopId = loc.getId();
                         if (returnPickupStopName == null || returnPickupStopName.isEmpty()) returnPickupStopName = (loc.getName() != null ? loc.getName() : loc.getAddress());
                     }
                 } catch (Exception ignored) {}
             }

             if ((returnDropoffStopId == -1 || returnDropoffStopName == null) && intent.hasExtra("return_dropoff_location")) {
                 try {
                     vn.hcmute.busbooking.model.Location loc = intent.getParcelableExtra("return_dropoff_location");
                     if (loc != null) {
                         if (returnDropoffStopId == -1) returnDropoffStopId = loc.getId();
                         if (returnDropoffStopName == null || returnDropoffStopName.isEmpty()) returnDropoffStopName = (loc.getName() != null ? loc.getName() : loc.getAddress());
                     }
                 } catch (Exception ignored) {}
             }

             // Debug logs to help diagnose missing return leg
             Log.d(TAG, "collectIntentData: returnTrip=" + (returnTrip != null ? returnTrip.getId() : "null") +
                      ", returnSeatLabels=" + (returnSeatLabels != null ? returnSeatLabels.size() : "null") +
                      ", returnPickupStopName=" + (returnPickupStopName != null ? returnPickupStopName : "null") +
                      ", returnDropoffStopName=" + (returnDropoffStopName != null ? returnDropoffStopName : "null"));

         } catch (Exception ignored) {
             Log.d(TAG, "collectIntentData: exception reading return extras: " + ignored);
         }

         // If this is a pending payment flow we allow missing trip/seatLabels because we'll fetch booking details from server
         if (!isPendingPayment) {
             // Relax validation: accept if we have either (id AND name) OR at least a name for both pickup and dropoff
             boolean pickupOk = (pickupStopId != -1 && pickupStopName != null && !pickupStopName.isEmpty()) || (pickupStopName != null && !pickupStopName.isEmpty());
             boolean dropoffOk = (dropoffStopId != -1 && dropoffStopName != null && !dropoffStopName.isEmpty()) || (dropoffStopName != null && !dropoffStopName.isEmpty());

             if (trip == null || seatLabels == null || seatLabels.isEmpty() || !pickupOk || !dropoffOk) {
                 Log.e(TAG, "Missing critical booking data from Intent. trip=" + (trip != null ? trip.getId() : "null") +
                         ", seats=" + (seatLabels != null ? seatLabels.size() : 0) +
                         ", pickupOk=" + pickupOk + ", dropoffOk=" + dropoffOk);
                 return false;
             }
         }

        // Contact info: accept both legacy keys and ContactInfoActivity keys
        fullName = intent.getStringExtra("passenger_name");
        if (fullName == null) fullName = intent.getStringExtra("fullName");
        phoneNumber = intent.getStringExtra("passenger_phone");
        if (phoneNumber == null) phoneNumber = intent.getStringExtra("phoneNumber");
        email = intent.getStringExtra("passenger_email");
        if (email == null) email = intent.getStringExtra("email");

         if (fullName == null) fullName = sessionManager.getUserName();
         if (phoneNumber == null) phoneNumber = sessionManager.getUserPhone();
         if (email == null) email = sessionManager.getUserEmail();

         return true;
     }

    private void populateBookingSummary() {
        // ============ HIỂN THỊ THÔNG TIN CHIỀU ĐI ============
        if (trip != null) {
            tvBusOperator.setText(trip.getOperator());
            tvBusType.setText(trip.getBusType());
            if (tvOrigin != null) tvOrigin.setText(trip.getOrigin());
            if (tvDestination != null) tvDestination.setText(trip.getDestination());
            tvDepartureTime.setText(formatDisplayDateTime(trip.getDepartureTime()));
        }

        if (tvAppName != null) tvAppName.setText(R.string.logo_default);

        // Hiển thị điểm đón/trả chiều đi
        displayStopInfo(tvPickup, pickupStopName);
        displayStopInfo(tvDropoff, dropoffStopName);

        // Hiển thị ghế (bao gồm cả chiều về nếu có)
        String seatText = "";
        if (seatLabels != null && !seatLabels.isEmpty()) {
            seatText = "Đi: " + TextUtils.join(", ", seatLabels);
        }

        if (returnTrip != null && returnSeatLabels != null && !returnSeatLabels.isEmpty()) {
            if (!seatText.isEmpty()) seatText += "\n";
            seatText += "Về: " + TextUtils.join(", ", returnSeatLabels);
        }

        if (tvSeat != null) tvSeat.setText(seatText);

        // ============ HIỂN THỊ THÔNG TIN CHIỀU VỀ (NẾU CÓ) ============
        populateReturnTripInfo();

        // Thông tin hành khách
        if (fullName != null && !fullName.isEmpty()) {
            tvPassengerName.setText(fullName);
        } else if (sessionManager.getUserName() != null) {
            tvPassengerName.setText(sessionManager.getUserName());
        }

        String phoneToShow = (phoneNumber != null) ? phoneNumber : sessionManager.getUserPhone();
        if (phoneToShow != null) {
            tvPassengerPhone.setText(phoneToShow);
        }

        // ============ TÍNH TỔNG TIỀN (BAO GỒM CẢ 2 CHIỀU) ============
        try {
            double departPrice = 0.0;
            double returnPrice = 0.0;

            // Tính tiền chiều đi
            if (trip != null && seatLabels != null && !seatLabels.isEmpty()) {
                departPrice = trip.getPrice() * seatLabels.size();
                Log.d(TAG, "Depart price: " + departPrice + " (seats: " + seatLabels.size() + ")");
            }

            // Tính tiền chiều về
            if (returnTrip != null && returnSeatLabels != null && !returnSeatLabels.isEmpty()) {
                returnPrice = returnTrip.getPrice() * returnSeatLabels.size();
                Log.d(TAG, "Return price: " + returnPrice + " (seats: " + returnSeatLabels.size() + ")");
            }

            bookingTotalAmount = departPrice + returnPrice;
            Log.d(TAG, "Total booking amount: " + bookingTotalAmount);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total amount", e);
        }

        updatePricingUI();

        // Fetch và hiển thị thời gian đón/trả ước tính
        try {
            if (trip != null && (pickupStopId > 0 || dropoffStopId > 0)) {
                fetchAndShowStopTimes(trip.getId(), pickupStopId, dropoffStopId);
            }
            // UPDATED: ensure return leg updates its own return-specific views
            if (returnTrip != null && (returnPickupStopId > 0 || returnDropoffStopId > 0)) {
                fetchAndShowStopTimes(returnTrip.getId(), returnPickupStopId, returnDropoffStopId, true);
            }
         } catch (Exception ignored) {}
    }

    // New helper: populate the dedicated return-trip card views safely
    private void populateReturnTripInfo() {
        // Find views
        TextView tvReturnInfoOld = findViewById(R.id.tvReturnTripInfo); // legacy single-text display
        TextView tvReturnLabel = findViewById(R.id.tvReturnLabel);
        LinearLayout layoutReturnTrip = findViewById(R.id.layoutReturnTrip);
        MaterialCardView cardReturn = findViewById(R.id.cardReturnTripInfo);

        // If no return trip selected, hide return section (both new card and legacy view)
        if (returnTrip == null || returnSeatLabels == null || returnSeatLabels.isEmpty()) {
            if (tvReturnLabel != null) tvReturnLabel.setVisibility(View.GONE);
            if (layoutReturnTrip != null) layoutReturnTrip.setVisibility(View.GONE);
            if (cardReturn != null) cardReturn.setVisibility(View.GONE);
            if (tvReturnInfoOld != null) tvReturnInfoOld.setVisibility(View.GONE);
            return;
        }

        // Show return section
        if (tvReturnLabel != null) tvReturnLabel.setVisibility(View.VISIBLE);
        if (layoutReturnTrip != null) layoutReturnTrip.setVisibility(View.VISIBLE);
        if (cardReturn != null) cardReturn.setVisibility(View.VISIBLE);
        if (tvReturnInfoOld != null) tvReturnInfoOld.setVisibility(View.GONE); // hide legacy

        // Populate the new return-trip card fields
        TextView tvReturnBusType = findViewById(R.id.tvReturnBusType);
        TextView tvReturnBusOperator = findViewById(R.id.tvReturnBusOperator);
        TextView tvReturnOrigin = findViewById(R.id.tvReturnOrigin);
        TextView tvReturnDestination = findViewById(R.id.tvReturnDestination);
        TextView tvReturnDepartureTime = findViewById(R.id.tvReturnDepartureTime);
        TextView tvReturnPickupLocation = findViewById(R.id.tvReturnPickupLocation);
        TextView tvReturnPickupAddress = findViewById(R.id.tvReturnPickupAddress);
        TextView tvReturnDropoffLocation = findViewById(R.id.tvReturnDropoffLocation);
        TextView tvReturnDropoffAddress = findViewById(R.id.tvReturnDropoffAddress);
        TextView tvReturnSeatNumber = findViewById(R.id.tvSeatNumber); // reuse main seat text view for combined display

        if (tvReturnPickupLocation != null && tvReturnPickupAddress != null) {
            tvReturnPickupLocation.setTag(tvReturnPickupAddress);
        }
        if (tvReturnDropoffLocation != null && tvReturnDropoffAddress != null) {
            tvReturnDropoffLocation.setTag(tvReturnDropoffAddress);
        }

        try {
            if (tvReturnBusType != null) tvReturnBusType.setText(returnTrip.getBusType());
            if (tvReturnBusOperator != null) tvReturnBusOperator.setText(returnTrip.getOperator());
            if (tvReturnOrigin != null) tvReturnOrigin.setText(returnTrip.getOrigin());
            if (tvReturnDestination != null) tvReturnDestination.setText(returnTrip.getDestination());
            if (tvReturnDepartureTime != null) tvReturnDepartureTime.setText(formatDisplayDateTime(returnTrip.getDepartureTime()));

            // Display pickup/dropoff names on the dedicated fields
            displayStopInfo(tvReturnPickupLocation, returnPickupStopName);
            displayStopInfo(tvReturnDropoffLocation, returnDropoffStopName);

            // Also update the combined seat text (tvSeat / tvSeatNumber already set in populateBookingSummary),
            // but ensure tvReturnSeatNumber shows combined text where applicable
            if (tvReturnSeatNumber != null) {
                String combinedSeats = "";
                if (seatLabels != null && !seatLabels.isEmpty()) combinedSeats = "Đi: " + TextUtils.join(", ", seatLabels);
                if (returnSeatLabels != null && !returnSeatLabels.isEmpty()) {
                    if (!combinedSeats.isEmpty()) combinedSeats += "\n";
                    combinedSeats += "Về: " + TextUtils.join(", ", returnSeatLabels);
                }
                tvReturnSeatNumber.setText(combinedSeats);
            }

        } catch (Exception e) {
            Log.w(TAG, "populateReturnTripInfo: error populating return UI", e);
        }

        // Fetch and display estimated times for return trip stops if available
        try {
            if (returnTrip != null && (returnPickupStopId > 0 || returnDropoffStopId > 0)) {
                fetchAndShowStopTimes(returnTrip.getId(), returnPickupStopId, returnDropoffStopId, true);
            }
        } catch (Exception ignored) {}

        Log.d(TAG, "populateReturnTripInfo: return trip UI updated (returnTripId=" + (returnTrip != null ? returnTrip.getId() : "null") + ")");
    }

    // Helper method để hiển thị thông tin điểm đón/trả
    private void displayStopInfo(TextView tv, String stopName) {
        if (tv == null || stopName == null) return;

        if (stopName.contains(" - ")) {
            String[] parts = stopName.split(" - ", 2);
            tv.setText(parts[0]);
            Object tag = tv.getTag();
            if (tag instanceof TextView) {
                TextView addrView = (TextView) tag;
                String addr = parts.length > 1 ? parts[1] : "";
                if (addr != null && !addr.isEmpty()) {
                    addrView.setText(addr);
                    addrView.setVisibility(View.VISIBLE);
                } else {
                    addrView.setVisibility(View.GONE);
                }
            }
        } else {
            tv.setText(stopName);
            Object tag = tv.getTag();
            if (tag instanceof TextView) {
                ((TextView) tag).setVisibility(View.GONE);
            }
        }
    }

    // Overload for backward-compatibility (defaults to depart/main trip)
    private void fetchAndShowStopTimes(int tripId, int pickupId, int dropoffId) {
        fetchAndShowStopTimes(tripId, pickupId, dropoffId, false);
    }

    // New: support specifying whether this is the return leg so we update the correct views
    private void fetchAndShowStopTimes(int tripId, int pickupId, int dropoffId, boolean isReturnTrip) {
        try {
            final TextView pickupTarget = isReturnTrip ? tvReturnPickupTimeView : tvPickupTimeView;
            final TextView dropoffTarget = isReturnTrip ? tvReturnDropoffTimeView : tvDropoffTimeView;

            ApiService service = apiService != null ? apiService : ApiClient.getClient().create(ApiService.class);

            if (pickupId > 0) {
                service.getPickupLocations(tripId).enqueue(new Callback<java.util.List<vn.hcmute.busbooking.model.Location>>() {
                    @Override public void onResponse(Call<java.util.List<vn.hcmute.busbooking.model.Location>> call, Response<java.util.List<vn.hcmute.busbooking.model.Location>> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                for (vn.hcmute.busbooking.model.Location loc : response.body()) {
                                    if (loc != null && loc.getId() == pickupId) {
                                        String iso = loc.getEstimatedTime();
                                        if (iso != null && !iso.isEmpty() && tvPickupTimeView != null) {
                                            tvPickupTimeView.setText(formatEstimatedTime(iso));
                                            tvPickupTimeView.setVisibility(View.VISIBLE);
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override public void onFailure(Call<java.util.List<vn.hcmute.busbooking.model.Location>> call, Throwable t) { /* ignore */ }
                });
            }

            if (dropoffId > 0) {
                service.getDropoffLocations(tripId).enqueue(new Callback<java.util.List<vn.hcmute.busbooking.model.Location>>() {
                    @Override public void onResponse(Call<java.util.List<vn.hcmute.busbooking.model.Location>> call, Response<java.util.List<vn.hcmute.busbooking.model.Location>> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                for (vn.hcmute.busbooking.model.Location loc : response.body()) {
                                    if (loc != null && loc.getId() == dropoffId) {
                                        String iso = loc.getEstimatedTime();
                                        if (iso != null && !iso.isEmpty() && tvDropoffTimeView != null) {
                                            tvDropoffTimeView.setText(formatEstimatedTime(iso));
                                            tvDropoffTimeView.setVisibility(View.VISIBLE);
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override public void onFailure(Call<java.util.List<vn.hcmute.busbooking.model.Location>> call, Throwable t) { /* ignore */ }
                });
            }
        } catch (Exception ignored) {}
    }


    // Restore payment selection & helper methods (were accidentally removed)
    private void selectPaymentMethod(int resId) {
         try {
             Log.d(TAG, "selectPaymentMethod called resId=" + resId + ", before currentPaymentMethod=" + currentPaymentMethod);
             // Default visuals/colors
             int selectedColor = android.graphics.Color.parseColor("#3075B0FD");
             int unselectedColor = android.graphics.Color.parseColor("#FFFFFFFF"); // white

             if (resId == R.id.rbCreditCard) {
                 if (rbCreditCard != null) rbCreditCard.setChecked(true);
                 if (rbQrPayment != null) rbQrPayment.setChecked(false);
                 if (rbPayAtOffice != null) rbPayAtOffice.setChecked(false);

                 if (creditCardForm != null) creditCardForm.setVisibility(View.VISIBLE);
                 if (cardCreditCard != null) cardCreditCard.setCardBackgroundColor(selectedColor);
                 if (cardQrPayment != null) cardQrPayment.setCardBackgroundColor(unselectedColor);
                 if (cardPayAtOffice != null) cardPayAtOffice.setCardBackgroundColor(unselectedColor);

                 currentPaymentMethod = "card";
                 isOfflinePayment = false;

             } else if (resId == R.id.rbQrPayment) {
                 if (rbQrPayment != null) rbQrPayment.setChecked(true);
                 if (rbCreditCard != null) rbCreditCard.setChecked(false);
                 if (rbPayAtOffice != null) rbPayAtOffice.setChecked(false);

                 if (creditCardForm != null) creditCardForm.setVisibility(View.GONE);
                 if (cardCreditCard != null) cardCreditCard.setCardBackgroundColor(unselectedColor);
                 if (cardQrPayment != null) cardQrPayment.setCardBackgroundColor(selectedColor);
                 if (cardPayAtOffice != null) cardPayAtOffice.setCardBackgroundColor(unselectedColor);

                 currentPaymentMethod = "qr";
                 isOfflinePayment = false;

             } else if (resId == R.id.rbPayAtOffice) {
                 if (rbPayAtOffice != null) rbPayAtOffice.setChecked(true);
                 if (rbCreditCard != null) rbCreditCard.setChecked(false);
                 if (rbQrPayment != null) rbQrPayment.setChecked(false);

                 if (creditCardForm != null) creditCardForm.setVisibility(View.GONE);
                 if (cardCreditCard != null) cardCreditCard.setCardBackgroundColor(unselectedColor);
                 if (cardQrPayment != null) cardQrPayment.setCardBackgroundColor(unselectedColor);
                 if (cardPayAtOffice != null) cardPayAtOffice.setCardBackgroundColor(selectedColor);

                 currentPaymentMethod = "offline";
                 isOfflinePayment = true;
                 // Clear card inputs and errors when switching to offline to avoid showing card as active
                 clearCardFields();
                 hideKeyboard();
             }

             Log.d(TAG, "selectPaymentMethod finished, currentPaymentMethod=" + currentPaymentMethod);

             // Update confirm button state based on current selection and inputs
             updateConfirmButtonState();
          } catch (Exception ignored) {}
     }

    private void clearCardFields() {
        try {
            if (etCardNumber != null) { etCardNumber.setText(""); etCardNumber.setError(null); }
            if (etExpiryDate != null) { etExpiryDate.setText(""); etExpiryDate.setError(null); }
            if (etCvv != null) { etCvv.setText(""); etCvv.setError(null); }
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() {
        try {
            View v = this.getCurrentFocus();
            if (v != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    // Start a visual countdown shown in cardCountdown -> tvCountdown. Cancels previous timer if running.
    private void startCountdown(long millisInFuture) {
        try {
            if (countDownTimer != null) {
                try { countDownTimer.cancel(); } catch (Exception ignored) {}
                countDownTimer = null;
            }
            if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
            long start = Math.max(0, millisInFuture);
            countDownTimer = new android.os.CountDownTimer(start, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    if (tvCountdown != null) tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
                }

                @Override
                public void onFinish() {
                    if (tvCountdown != null) tvCountdown.setText("");
                    if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                }
            };
            countDownTimer.start();
        } catch (Exception ignored) {}
    }

    private void processCashPayment(List<Integer> ids) {
        // For cash/offline payments, confirm bookings on server with 'offline' method.
        if (ids == null || ids.isEmpty()) {
            handlePaymentError("Không có mã đặt vé để xử lý thanh toán.");
            return;
        }
        // confirmNextPayment will set loading state and proceed sequentially
        confirmNextPayment(ids, 0, "offline");
    }

    private void setupPaymentMethodSelection() {
         try {
            if (rbCreditCard != null) rbCreditCard.setOnClickListener(v -> selectPaymentMethod(R.id.rbCreditCard));
            if (rbQrPayment != null) rbQrPayment.setOnClickListener(v -> selectPaymentMethod(R.id.rbQrPayment));
            if (rbPayAtOffice != null) rbPayAtOffice.setOnClickListener(v -> selectPaymentMethod(R.id.rbPayAtOffice));

            // Make the whole card clickable (users often tap the card, not the radio button)
            if (cardCreditCard != null) cardCreditCard.setOnClickListener(v -> selectPaymentMethod(R.id.rbCreditCard));
            if (cardQrPayment != null) cardQrPayment.setOnClickListener(v -> selectPaymentMethod(R.id.rbQrPayment));
            if (cardPayAtOffice != null) cardPayAtOffice.setOnClickListener(v -> selectPaymentMethod(R.id.rbPayAtOffice));

            // If currentPaymentMethod was set (e.g., from booking details), honor it as initial selection
            if (currentPaymentMethod != null) {
                String lower = currentPaymentMethod.toLowerCase(Locale.getDefault());
                if (lower.contains("card")) selectPaymentMethod(R.id.rbCreditCard);
                else if (lower.contains("qr") || lower.contains("vnpay") || lower.contains("payos")) selectPaymentMethod(R.id.rbQrPayment);
                else if (lower.contains("offline") || lower.contains("cash") || lower.contains("counter")) selectPaymentMethod(R.id.rbPayAtOffice);
            }

             // Initialize credit-card validators
             setupCreditCardFormValidators();
         } catch (Exception ignored) {}
    }

    private void setupPromoHandlers() {
        try {
            if (btnApplyPromo != null) btnApplyPromo.setOnClickListener(v -> {
                String code = etPromoCode != null ? etPromoCode.getText().toString().trim() : null;
                if (code == null || code.isEmpty()) {
                    Toast.makeText(PaymentActivity.this, "Vui lòng nhập mã khuyến mãi", Toast.LENGTH_SHORT).show();
                    return;
                }
                validateAndApplyPromo(code);
            });

            if (tvSelectPromo != null) {
                tvSelectPromo.setOnClickListener(v -> showPromotionsBottomSheet());
            }

            // Setup coin switch listener
            if (switchUseCoins != null) {
                switchUseCoins.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    updatePricingUI();
                });
            }
        } catch (Exception ignored) {}
    }


    private void fetchBookingDetailsAndInit(Integer bookingId) {
        if (bookingId == null) {
            handlePaymentError("Thiếu mã đặt vé.");
            return;
        }
        setLoadingState(true);
        apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    try {
                        // Populate passenger contact info from booking if available
                        try {
                            Object pObj = data.get("passenger_info");
                            if (pObj instanceof Map) {
                                Map<?, ?> pm = (Map<?, ?>) pObj;
                                Object n = pm.get("name");
                                Object ph = pm.get("phone");
                                Object em = pm.get("email");
                                if (n != null) fullName = String.valueOf(n);
                                if (ph != null) phoneNumber = String.valueOf(ph);
                                if (em != null) email = String.valueOf(em);
                            } else if (pObj instanceof String) {
                                try {
                                    JSONObject jo = new JSONObject((String) pObj);
                                    fullName = jo.optString("name", fullName);
                                    phoneNumber = jo.optString("phone", phoneNumber);
                                    email = jo.optString("email", email);
                                } catch (Exception ignored) {}
                            } else {
                                // Fallback to legacy fields
                                Object n = data.get("passenger_name");
                                Object ph = data.get("passenger_phone");
                                Object em = data.get("passenger_email");
                                if (n != null) fullName = String.valueOf(n);
                                if (ph != null) phoneNumber = String.valueOf(ph);
                                if (em != null) email = String.valueOf(em);
                            }
                        } catch (Exception ignored) {}
                        // Populate pickup/dropoff ids and names if present so UI can show stop times
                        try {
                            Object pId = data.get("pickup_stop_id");
                            if (pId instanceof Number) pickupStopId = ((Number)pId).intValue();
                            else if (pId instanceof String) try { pickupStopId = Integer.parseInt((String)pId); } catch (Exception ignored) {}
                            Object dId = data.get("dropoff_stop_id");
                            if (dId instanceof Number) dropoffStopId = ((Number)dId).intValue();
                            else if (dId instanceof String) try { dropoffStopId = Integer.parseInt((String)dId); } catch (Exception ignored) {}
                            Object pName = data.get("pickup_location"); if (pName != null && pickupStopName == null) pickupStopName = String.valueOf(pName);
                            Object dName = data.get("dropoff_location"); if (dName != null && dropoffStopName == null) dropoffStopName = String.valueOf(dName);
                        } catch (Exception ignored) {}
                         bookingIds = bookingIds == null ? new ArrayList<>() : bookingIds;
                         if (!bookingIds.contains(bookingId)) bookingIds.add(bookingId);
                         Object pm = data.get("payment_method");
                         currentPaymentMethod = pm == null ? null : String.valueOf(pm);
                         boolean online = (currentPaymentMethod != null && normalizePaymentMethod(currentPaymentMethod).equals("online"));
                         if (online) {
                            long expiry = System.currentTimeMillis() + HOLD_DURATION_MS;
                            saveExpiryForBooking(bookingId, expiry);
                            startCountdown(HOLD_DURATION_MS);
                         }
                    } catch (Exception ignored) {}
                    // Continue initialization
                    populateBookingSummary();
                    setupPaymentMethodSelection();
                    setupPromoHandlers();
                    loadUserCoinBalance(); // Load user's coin balance
                } else {
                    handlePaymentError("Không thể tải chi tiết vé.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                handlePaymentError("Lỗi kết nối khi tải vé: " + t.getMessage());
            }
        });
    }

    private void updateConfirmButtonState() {
        try {
            int sel = getSelectedPaymentMethodId();
            if (sel == R.id.rbCreditCard) {
                boolean ok = validateCardInputs();
                if (btnConfirmPayment != null) btnConfirmPayment.setEnabled(ok);
            } else {
                if (btnConfirmPayment != null) btnConfirmPayment.setEnabled(true);
            }
        } catch (Exception ignored) {}
    }

    private int getSelectedPaymentMethodId() {
        try {
            if (rbQrPayment != null && rbQrPayment.isChecked()) return R.id.rbQrPayment;
            if (rbCreditCard != null && rbCreditCard.isChecked()) return R.id.rbCreditCard;
            if (rbPayAtOffice != null && rbPayAtOffice.isChecked()) return R.id.rbPayAtOffice;
        } catch (Exception ignored) {}
        return R.id.rbQrPayment; // default
    }

    private boolean validateCardInputs() {
        if (etCardNumber == null || etExpiryDate == null || etCvv == null) return false;
        String rawPan = etCardNumber.getText() == null ? "" : etCardNumber.getText().toString();
        String pan = rawPan.replaceAll("[^0-9]", "");
        String expiry = etExpiryDate.getText() == null ? "" : etExpiryDate.getText().toString().trim();
        String cvv = etCvv.getText() == null ? "" : etCvv.getText().toString().trim();

        // Clear previous errors
        try { etCardNumber.setError(null); etExpiryDate.setError(null); etCvv.setError(null); } catch (Exception ignored) {}

         if (pan.isEmpty() || pan.length() < 12 || pan.length() > 19) {
            try { etCardNumber.requestFocus(); etCardNumber.setError("Số thẻ phải có ít nhất 16 chữ số"); } catch (Exception ignored) {}
            return false;
        }

        // New: Luhn checksum validation to catch obviously invalid card numbers early
        if (!luhnCheck(pan)) {
            try { etCardNumber.requestFocus(); etCardNumber.setError("Số thẻ không hợp lệ"); } catch (Exception ignored) {}
            return false;
        }

        if (!isExpiryValid(expiry)) {
            try { etExpiryDate.requestFocus(); etExpiryDate.setError("Ngày hết hạn không hợp lệ hoặc đã hết hạn (MM/YY)"); } catch (Exception ignored) {}
            return false;
        }
        if (!isCvvValid(cvv)) {
            try { etCvv.requestFocus(); etCvv.setError("CVV không hợp lệ (3–4 chữ số)"); } catch (Exception ignored) {}
            return false;
        }
        return true;
    }

    // Luhn algorithm (client-side) to validate PAN before sending to server
    private boolean luhnCheck(String number) {
        if (number == null) return false;
        String s = number.replaceAll("\\D", "");
        if (s.length() < 12 || s.length() > 19) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = s.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(s.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private void setupCreditCardFormValidators() {
        try {
            android.text.TextWatcher watcher = new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) { updateConfirmButtonState(); }
            };
            if (etCardNumber != null) etCardNumber.addTextChangedListener(watcher);
            if (etExpiryDate != null) etExpiryDate.addTextChangedListener(watcher);
            if (etCvv != null) etCvv.addTextChangedListener(watcher);

            // Consume IME actions (Done/Next) to prevent accidental form submission while typing
            if (etCardNumber != null) etCardNumber.setOnEditorActionListener((v, actionId, event) -> true);
            if (etExpiryDate != null) etExpiryDate.setOnEditorActionListener((v, actionId, event) -> true);
            if (etCvv != null) etCvv.setOnEditorActionListener((v, actionId, event) -> true);

            // Also update when radio changes
            if (rbCreditCard != null) rbCreditCard.setOnCheckedChangeListener((buttonView, isChecked) -> updateConfirmButtonState());
            if (rbQrPayment != null) rbQrPayment.setOnCheckedChangeListener((buttonView, isChecked) -> updateConfirmButtonState());
            if (rbPayAtOffice != null) rbPayAtOffice.setOnCheckedChangeListener((buttonView, isChecked) -> updateConfirmButtonState());

            updateConfirmButtonState();
        } catch (Exception ignored) {}
    }

    /**
     * Show a bottom sheet listing available promotions fetched from API.
     */
    private void showPromotionsBottomSheet() {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            View sheet = LayoutInflater.from(this).inflate(R.layout.layout_promotions_selection, null);
            RecyclerView rv = sheet.findViewById(R.id.rvPromotions);
            ProgressBar progress = sheet.findViewById(R.id.progressPromos);
            TextView tvEmpty = sheet.findViewById(R.id.tvEmptyPromos);

            rv.setLayoutManager(new LinearLayoutManager(this));
            dialog.setContentView(sheet);
            dialog.show();

            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

            apiService.getPromotions().enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
                @Override
                public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                    if (progress != null) progress.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        java.util.List<java.util.Map<String, Object>> body = response.body();
                        java.util.List<Promotion> promos = new java.util.ArrayList<>();
                        for (java.util.Map<String, Object> m : body) {
                            try { promos.add(new Promotion(m)); } catch (Exception ignored) {}
                        }
                        if (promos.isEmpty()) {
                            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            setupSelectionAdapter(rv, promos, dialog);
                        }
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                    if (progress != null) progress.setVisibility(View.GONE);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(PaymentActivity.this, "Không thể tải danh sách khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing promotions bottom sheet", e);
        }
    }

    private void setupSelectionAdapter(RecyclerView rv, java.util.List<Promotion> list, BottomSheetDialog dialog) {
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion_selection, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Promotion p = list.get(position);
                TextView title = holder.itemView.findViewById(R.id.tvPromoCode);
                TextView desc = holder.itemView.findViewById(R.id.tvPromoDesc);
                TextView minPrice = holder.itemView.findViewById(R.id.tvPromoMinPrice);
                TextView maxPrice = holder.itemView.findViewById(R.id.tvPromoMaxPrice);

                try {
                    if (title != null) title.setText(p.getCode() != null ? p.getCode() : p.getTitle());
                    if (desc != null) desc.setText(p.getDescription() != null ? p.getDescription() : "");

                    // Display minimum price
                    if (minPrice != null) {
                        double min = p.getMin_price();
                        if (min > 0) {
                            minPrice.setText(CurrencyUtil.formatVND(min));
                        } else {
                            minPrice.setText("Không giới hạn");
                        }
                    }

                    // Display maximum discount
                    if (maxPrice != null) {
                        double max = p.getMax_discount();
                        if (max > 0) {
                            maxPrice.setText(CurrencyUtil.formatVND(max));
                        } else {
                            maxPrice.setText("Không giới hạn");
                        }
                    }
                } catch (Exception ignored) {}

                holder.itemView.setOnClickListener(v -> {
                    try {
                        String code = p.getCode() != null ? p.getCode() : (p.getTitle() != null ? p.getTitle() : "");
                        if (etPromoCode != null) {
                            etPromoCode.setText(code);
                        }
                        // Automatically validate the selected promo code
                        validateAndApplyPromo(code);
                    } catch (Exception e) {
                        Log.e(TAG, "Error on promo item click", e);
                    }
                    try { dialog.dismiss(); } catch (Exception ignored) {}
                });
            }
            @Override public int getItemCount() { return list.size(); }
        });
    }

    private void validateAndApplyPromo(String code) {
        if (code == null || code.trim().isEmpty()) {
            Toast.makeText(this, "Mã khuyến mãi không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastApplyClickMs < 800) return;
        lastApplyClickMs = now;

        setLoadingState(true);
        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip != null ? trip.getPrice() * (seatLabels != null ? seatLabels.size() : 0) : 0);

        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("amount", subtotal);

        apiService.validatePromotion(body).enqueue(new Callback<vn.hcmute.busbooking.model.PromotionValidateResponse>() {
            @Override
            public void onResponse(Call<vn.hcmute.busbooking.model.PromotionValidateResponse> call, Response<vn.hcmute.busbooking.model.PromotionValidateResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    vn.hcmute.busbooking.model.PromotionValidateResponse resp = response.body();
                    if (resp.isValid()) {
                        appliedPromotionCode = code;
                        appliedDiscount = resp.getDiscount();
                        appliedPromotion = new HashMap<>();
                        if (resp.getPromotion() != null) {
                            appliedPromotion.put("id", resp.getPromotion().getId());
                            appliedPromotion.put("code", resp.getPromotion().getCode());
                        }
                        if (tvPromoDetails != null) tvPromoDetails.setText("Đã áp dụng: " + code + " (-" + CurrencyUtil.formatVND(appliedDiscount) + ")");
                        if (tvDiscountApplied != null) tvDiscountApplied.setText("-" + CurrencyUtil.formatVND(appliedDiscount));
                        updatePricingUI();
                        Toast.makeText(PaymentActivity.this, "Áp dụng mã thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        String reason = resp.getReason() != null ? resp.getReason() : "Mã không hợp lệ";
                        Toast.makeText(PaymentActivity.this, reason, Toast.LENGTH_LONG).show();
                        appliedPromotionCode = null;
                        appliedDiscount = 0.0;
                        updatePricingUI();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, "Lỗi khi xác thực mã", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<vn.hcmute.busbooking.model.PromotionValidateResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(PaymentActivity.this, "Lỗi kết nối khi xác thực mã: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private long parseIsoToMillis(String iso) {
        if (iso == null) return -1;
        String[] patterns = new String[] {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.getDefault());
                if (p.contains("'Z'") || p.contains("X")) sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        // fallback: try numeric
        try {
            String s = iso.trim();
            if (s.matches("^\\d+$")) {
                long v = Long.parseLong(s);
                if (v < 100000000000L) v = v * 1000L;
                return v;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    // New helper: detect timezone information from the ISO string. If the ISO contains
    // an explicit timezone (Z or an offset like +07:00), return a matching TimeZone so
    // formatting can preserve the original clock time instead of converting to device tz.
    private java.util.TimeZone getTimeZoneFromIso(String iso) {
        if (iso == null) return null;
        String s = iso.trim();
        if (s.isEmpty()) return null;
        // Zulu (UTC)
        if (s.endsWith("Z") || s.endsWith("z")) {
            return java.util.TimeZone.getTimeZone("UTC");
        }
        // Match offsets like +07:00, -05:30, +0700, -0530 at the end of string
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([+-]\\d{2}:?\\d{2})$").matcher(s);
        if (m.find()) {
            String off = m.group(1);
            // Normalize to +HH:MM
            if (!off.contains(":")) {
                if (off.length() == 5) off = off.substring(0, 3) + ":" + off.substring(3);
            }
            return java.util.TimeZone.getTimeZone("GMT" + off);
        }
        return null;
    }

    private String formatDisplayTime(String isoString) {
        if (isoString == null) return "";
        try {
            long millis = parseIsoToMillis(isoString);
            if (millis <= 0) return "";
            Date date = new Date(millis);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return timeFormat.format(date);
        } catch (Exception e) { return ""; }
    }

    private String formatEstimatedTime(String isoString) {
        if (isoString == null) return "";
        try {
            long millis = parseIsoToMillis(isoString);
            if (millis <= 0) return "";
            Date date = new Date(millis);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            // DO NOT set timezone - estimated_time is already in local format (no UTC indicator)
            return timeFormat.format(date);
        } catch (Exception e) { return ""; }
    }

    private String formatDisplayDateTime(String isoString) {
        if (isoString == null) return "";
        try {
            long millis = parseIsoToMillis(isoString);
            if (millis <= 0) return "";
            Date date = new Date(millis);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            // Set timezone to UTC to match the incoming data
            timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return timeFormat.format(date) + " • " + dateFormat.format(date);
        } catch (Exception e) { return ""; }
    }

    private String maskPhone(String phone) {
        if (phone == null) return "";
        String s = phone.replaceAll("\\s+", "");
        if (s.length() <= 4) return s;
        int len = s.length();
        int keep = Math.max(2, len/4);
        String start = s.substring(0, keep);
        String end = s.substring(len - keep);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len - keep*2; i++) sb.append('*');
        return start + sb.toString() + end;
    }

    private void updatePricingUI() {
        try {
            // Calculate original subtotal (before any discounts)
            double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip != null ? trip.getPrice() * (seatLabels != null ? seatLabels.size() : 0) : 0);

            // Apply promotion discount first
            double priceAfterPromo = subtotal - appliedDiscount;
            priceAfterPromo = Math.max(0, priceAfterPromo);

            // Apply coin discount on remaining amount (1 coin = 1 VND)
            double coinDiscount = 0;
            if (switchUseCoins != null && switchUseCoins.isChecked() && userCoinBalance > 0) {
                // Use all available coins or up to the remaining amount
                coinDiscount = Math.min(userCoinBalance, priceAfterPromo);
            }

            // Final price after both discounts
            double finalPrice = Math.max(0, priceAfterPromo - coinDiscount);

            // Display original subtotal (unchanged)
            if (tvSubtotal != null) tvSubtotal.setText(CurrencyUtil.formatVND(subtotal));

            // Show/Hide Promotion Discount layout
            LinearLayout layoutPromoDiscount = findViewById(R.id.layoutPromoDiscount);
            if (appliedDiscount > 0 && tvDiscountApplied != null) {
                if (layoutPromoDiscount != null) layoutPromoDiscount.setVisibility(View.VISIBLE);
                tvDiscountApplied.setText("-" + CurrencyUtil.formatVND(appliedDiscount));
            } else {
                if (layoutPromoDiscount != null) layoutPromoDiscount.setVisibility(View.GONE);
            }

            // Show/Hide Coin Discount layout
            LinearLayout layoutCoinDiscount = findViewById(R.id.layoutCoinDiscount);
            if (coinDiscount > 0) {
                if (layoutCoinDiscount != null) layoutCoinDiscount.setVisibility(View.VISIBLE);
                TextView tvCoinDiscount = findViewById(R.id.tvCoinDiscount);
                if (tvCoinDiscount != null) {
                    tvCoinDiscount.setText("-" + CurrencyUtil.formatVND(coinDiscount));
                }
            } else {
                if (layoutCoinDiscount != null) layoutCoinDiscount.setVisibility(View.GONE);
            }

            // Display final total after all discounts
            if (tvTotal != null) tvTotal.setText(CurrencyUtil.formatVND(finalPrice));
            if (tvBottomTotal != null) tvBottomTotal.setText(CurrencyUtil.formatVND(finalPrice));
        } catch (Exception ignored) {}
    }

    // Calculate how many coins will be used based on current settings
    private int calculateUsedCoins() {
        if (switchUseCoins == null || !switchUseCoins.isChecked() || userCoinBalance == 0) {
            Log.d(TAG, "❌ calculateUsedCoins: switchUseCoins=null?" + (switchUseCoins == null) +
                   ", isChecked?" + (switchUseCoins != null ? switchUseCoins.isChecked() : "null") +
                   ", userCoinBalance=" + userCoinBalance);
            return 0;
        }

        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip != null ? trip.getPrice() * (seatLabels != null ? seatLabels.size() : 0) : 0);
        double afterPromoDiscount = subtotal - appliedDiscount;
        afterPromoDiscount = Math.max(0, afterPromoDiscount);

        int coinToUse = (int) Math.min(userCoinBalance, afterPromoDiscount);
        Log.d(TAG, "✅ calculateUsedCoins: subtotal=" + subtotal + ", appliedDiscount=" + appliedDiscount +
               ", afterPromo=" + afterPromoDiscount + ", userBalance=" + userCoinBalance + ", coinToUse=" + coinToUse);

        // Use coins up to the remaining amount (1 coin = 1 VND)
        return coinToUse;
    }

    private void loadUserCoinBalance() {
        try {
            Integer userId = sessionManager.getUserId();
            if (userId == null) {
                if (tvUserCoinBalance != null) {
                    tvUserCoinBalance.setText("Bạn có 0 xu");
                }
                return;
            }

            apiService.getCoinBalance(userId).enqueue(new Callback<Map<String, Integer>>() {
                @Override
                public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Integer balance = response.body().get("balance");
                        userCoinBalance = balance != null ? balance : 0;

                        if (tvUserCoinBalance != null) {
                            tvUserCoinBalance.setText("Bạn có " + String.format("%,d", userCoinBalance) + " xu");
                        }

                        // Enable/disable coin switch based on balance
                        if (switchUseCoins != null) {
                            switchUseCoins.setEnabled(userCoinBalance > 0);
                            if (userCoinBalance == 0) {
                                switchUseCoins.setChecked(false);
                            }
                        }

                        // Update pricing UI to reflect current coin balance
                        updatePricingUI();
                    } else {
                        if (tvUserCoinBalance != null) {
                            tvUserCoinBalance.setText("Bạn có 0 xu");
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Integer>> call, Throwable t) {
                    Log.e(TAG, "Failed to load coin balance", t);
                    if (tvUserCoinBalance != null) {
                        tvUserCoinBalance.setText("Bạn có 0 xu");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading coin balance", e);
        }
    }


    private String normalizePaymentMethod(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.getDefault());
        if (lower.contains("card") || lower.contains("qr") || lower.contains("vnpay") || lower.contains("stripe") || lower.contains("payos") ) return "online";
        return "offline";
    }

    private void saveExpiryForBooking(Integer bookingId, long expiryMs) {
        try {
            if (bookingId == null) return;
            android.content.SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            sp.edit().putLong("booking_expires_" + bookingId, expiryMs).apply();
        } catch (Exception ignored) {}
    }

    // Minimal stubs for payment flows so file compiles. These should be replaced with full implementations.
    private void processPayosPayment(java.util.List<Integer> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                Toast.makeText(this, "Không có vé để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bookingTotalAmount == null) {
                Toast.makeText(this, "Không xác định được số tiền thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }

            // If amount is 0 (fully discounted), confirm payment to prevent auto-cancel by cron
            if (bookingTotalAmount <= 0) {
                Log.d(TAG, "Amount is 0 (fully discounted), confirming payment for booking");
                setLoadingState(true);

                // Confirm payment with 0 amount to mark booking as paid
                confirmNextPayment(ids, 0, "free");
                return;
            }

            setLoadingState(true);

            long timestamp = System.currentTimeMillis();
            int random = new java.util.Random().nextInt(10000);
            long orderId = timestamp + random;

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", orderId);
            paymentRequest.put("amount", bookingTotalAmount.intValue());
            paymentRequest.put("booking_ids", ids);
            paymentRequest.put("buyerName", fullName != null ? fullName : "Customer");
            paymentRequest.put("buyerEmail", email != null ? email : "customer@xekhachbooking.com");
            paymentRequest.put("buyerPhone", phoneNumber != null ? phoneNumber : "0123456789");
            paymentRequest.put("buyerAddress", "Vietnam");

            Log.d(TAG, "Creating PayOS checkout with amount: " + bookingTotalAmount + ", booking_ids: " + ids);

            Call<PaymentResponse> call = apiService.createPayosPayment(new vn.hcmute.busbooking.model.PaymentRequest(
                String.valueOf(orderId),
                bookingTotalAmount.intValue(),
                ids,
                fullName,
                email,
                phoneNumber
            ));

            call.enqueue(new Callback<PaymentResponse>() {
                @Override
                public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                    setLoadingState(false);

                    if (response.isSuccessful() && response.body() != null) {
                        String checkoutUrl = response.body().getCheckoutUrl();

                        if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                            Log.d(TAG, "Got PayOS checkout URL, opening browser");
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                            startActivity(browserIntent);
                        } else {
                            handlePaymentError("Không nhận được link thanh toán từ server");
                        }
                    } else {
                        handlePaymentError("Lỗi khi tạo link thanh toán: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<PaymentResponse> call, Throwable t) {
                    setLoadingState(false);
                    handlePaymentError("Lỗi kết nối thanh toán: " + t.getMessage());
                    Log.e(TAG, "PayOS request failed", t);
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            handlePaymentError("Lỗi: " + e.getMessage());
            Log.e(TAG, "Error in processPayosPayment", e);
        }
    }

    private void confirmNextPayment(java.util.List<Integer> ids, int index, String method) {
        if (ids == null || ids.isEmpty() || index >= ids.size()) {
            if (ids != null && !ids.isEmpty()) {
                onAllPaymentsSuccess(ids.get(0));
            }
            return;
        }

        if ("card".equalsIgnoreCase(method) && !validateCardInputs()) {
            handlePaymentError("Vui lòng nhập đầy đủ thông tin thẻ");
            return;
        }

        setLoadingState(true);
        Integer bookingId = ids.get(index);

        Map<String, String> body = new HashMap<>();
        body.put("payment_method", method);

        // If paying by card, include card fields (server requires them)
        if ("card".equalsIgnoreCase(method)) {
            try {
                String rawPan = etCardNumber == null ? "" : (etCardNumber.getText() == null ? "" : etCardNumber.getText().toString());
                String pan = rawPan.replaceAll("[^0-9]", "");
                String expiry = etExpiryDate == null ? "" : (etExpiryDate.getText() == null ? "" : etExpiryDate.getText().toString().trim());
                String cvv = etCvv == null ? "" : (etCvv.getText() == null ? "" : etCvv.getText().toString().trim());

                // Put sanitized values expected by backend
                body.put("card_number", pan);
                body.put("expiry", expiry);
                body.put("cvv", cvv);
            } catch (Exception ignored) {}
        }

        Log.d(TAG, "Processing payment for booking: " + bookingId + " with method: " + method + " body: " + body);

        apiService.confirmPayment(bookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Payment successful for booking " + bookingId);

                    if (index + 1 < ids.size()) {
                        confirmNextPayment(ids, index + 1, method);
                    } else {
                        setLoadingState(false);
                        onAllPaymentsSuccess(bookingId);
                    }
                } else {
                    setLoadingState(false);
                    String errMsg = "Thanh toán thất bại: " + response.code();
                    try {
                        if (response.errorBody() != null) errMsg += " " + response.errorBody().string();
                    } catch (Exception ignored) {}
                    handlePaymentError(errMsg);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                handlePaymentError("Lỗi kết nối thanh toán: " + t.getMessage());
                Log.e(TAG, "Payment request failed", t);
            }
        });
    }

    // Helper methods
    private void handlePaymentError(String errorMessage) {
        Log.e(TAG, "Payment error: " + errorMessage);
        try {
            Toast.makeText(PaymentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {}
    }

    private void setLoadingState(boolean loading) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
            if (btnConfirmPayment != null) {
                btnConfirmPayment.setEnabled(!loading);
            }
        } catch (Exception ignored) {}
    }

    private void onAllPaymentsSuccess(int primaryBookingId) {
        Log.d(TAG, "All payments completed successfully");
        Log.d(TAG, "usedCoinAmount = " + usedCoinAmount + ", bookingId = " + primaryBookingId);

        // If coins were used, deduct them from user account
        if (usedCoinAmount > 0) {
            Log.d(TAG, "Deducting coins - usedCoinAmount: " + usedCoinAmount);
            deductUserCoins(primaryBookingId);
        } else {
            Log.d(TAG, "No coins to deduct - usedCoinAmount: " + usedCoinAmount);
        }

        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

        // Delay 2 seconds then navigate to MyBookingsActivity to show the new booking
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to MyBookingsActivity", e);
            }
            finish();
        }, 2000);
    }

    // Deduct coins from user and record in coin_history
    private void deductUserCoins(int bookingId) {
        deductUserCoins(bookingId, null);
    }

    // Deduct coins from user and record in coin_history with optional callback
    private void deductUserCoins(int bookingId, Runnable onComplete) {
        try {
            Integer userId = sessionManager.getUserId();
            Log.d(TAG, "📥 deductUserCoins called: userId=" + userId + ", usedCoinAmount=" + usedCoinAmount + ", bookingId=" + bookingId);

            if (userId == null || usedCoinAmount <= 0) {
                Log.d(TAG, "⚠️  Skipping coin deduction: userId null=" + (userId == null) + ", usedCoinAmount=" + usedCoinAmount);
                // Still call the callback even if no coins to deduct
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("amount", usedCoinAmount);
            body.put("booking_id", bookingId);

            Log.d(TAG, "🪙 Calling API /coins/use to deduct " + usedCoinAmount + " coins for booking " + bookingId + " from user " + userId);
            Log.d(TAG, "🪙 Request body: " + body.toString());

            apiService.useCoins(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    Log.d(TAG, "🪙 API Response received - code: " + response.code() + ", successful: " + response.isSuccessful());
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Coins deducted successfully: " + usedCoinAmount + " coins for booking " + bookingId);
                        Log.d(TAG, "✅ Response body: " + response.body());
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "no body";
                            Log.e(TAG, "❌ Failed to deduct coins: code=" + response.code() + ", message=" + response.message() + ", errorBody=" + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Failed to deduct coins: " + response.code() + ", message: " + response.message());
                        }
                    }
                    // Call callback regardless of success/failure to continue flow
                    if (onComplete != null) {
                        Log.d(TAG, "🪙 Coin deduction complete, calling callback");
                        onComplete.run();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "❌ Error deducting coins (API call failed): " + t.getMessage(), t);
                    // Call callback to continue flow even on failure
                    if (onComplete != null) {
                        Log.d(TAG, "🪙 Coin deduction failed, calling callback to continue");
                        onComplete.run();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in deductUserCoins: " + e.getMessage(), e);
            e.printStackTrace();
            // Call callback to continue flow even on exception
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }


    private boolean isExpiryValid(String expiry) {
        if (expiry == null) return false;
        expiry = expiry.replaceAll("\\s", "");
        String[] parts = expiry.contains("/") ? expiry.split("/") : (expiry.length() == 4 ? new String[] {expiry.substring(0,2), expiry.substring(2)} : null);
        if (parts == null || parts.length < 2) return false;
        try {
            int m = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            if (y < 100) y += 2000;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.MONTH, m-1);
            cal.set(java.util.Calendar.YEAR, y);
            java.util.Calendar end = (java.util.Calendar) cal.clone();
            end.add(java.util.Calendar.MONTH, 1);
            end.add(java.util.Calendar.DAY_OF_MONTH, -1);
            return end.getTimeInMillis() > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCvvValid(String cvv) {
        if (cvv == null) return false;
        String s = cvv.replaceAll("\\D", "");
        return s.length() >= 3 && s.length() <= 4;
    }

    private void changePaymentMethodToOffline() {
        try {
            changePaymentMethodToOfflineConfirm("offline");
        } catch (Exception ignored) {}
    }

    private void changePaymentMethodToOfflineConfirm(String newMethod) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            handlePaymentError("Không có mã đặt vé để đổi phương thức thanh toán.");
            return;
        }

        setLoadingState(true);
        Map<String, String> body = new HashMap<>();
        body.put("payment_method", "offline");

        Integer bookingId = bookingIds.get(0);
        Log.d(TAG, "Changing payment method to offline for booking: " + bookingId);

        apiService.changePaymentMethod(bookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoadingState(false);

                if (response.isSuccessful()) {
                    Log.d(TAG, "Payment method changed to offline successfully");
                    isOfflinePayment = true;

                    Toast.makeText(PaymentActivity.this,
                            "Vé đã được đặt với thanh toán tại nhà xe. Vui lòng thanh toán trước khi lên xe.",
                            Toast.LENGTH_LONG).show();

                    // DO NOT deduct coins for offline payment here. Coins will be deducted when payment is confirmed.
                    Log.d(TAG, "Offline payment: coins will NOT be deducted yet (usedCoinAmount=" + usedCoinAmount + ")");
                    Log.d(TAG, "Offline payment: Coins will be deducted only when payment is confirmed by user");

                    // Finish immediately without deducting coins
                    Intent resultIntent = new Intent();
                    resultIntent.putIntegerArrayListExtra("booking_ids", bookingIds);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    handlePaymentError("Không thể thay đổi phương thức thanh toán: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                handlePaymentError("Lỗi kết nối khi thay đổi phương thức: " + t.getMessage());
            }
        });
    }

    private void createBookingAndProcessPayment() {
        if (trip == null || seatLabels == null || seatLabels.isEmpty()) {
            handlePaymentError("Dữ liệu đặt vé không đầy đủ");
            return;
        }

        if (fullName == null || fullName.isEmpty()) {
            handlePaymentError("Vui lòng nhập tên hành khách");
            return;
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            handlePaymentError("Vui lòng nhập số điện thoại");
            return;
        }

        setLoadingState(true);

        Integer userId = sessionManager.getUserId();

        // Build depart booking request
        Map<String, Object> departRequest = new HashMap<>();
        if (userId != null) { departRequest.put("user_id", userId);
        } else {
            // Gửi 1 để báo đây là Guest (nếu server bắt buộc trường user_id)
            departRequest.put("user_id", 1);
        }
        departRequest.put("trip_id", trip.getId());
        departRequest.put("seat_labels", seatLabels);
        departRequest.put("passenger_name", fullName);
        departRequest.put("passenger_phone", phoneNumber);
        departRequest.put("passenger_email", email != null ? email : "");
        departRequest.put("pickup_stop_id", pickupStopId);
        departRequest.put("dropoff_stop_id", dropoffStopId);

        if (appliedPromotionCode != null && !appliedPromotionCode.isEmpty()) {
            departRequest.put("promotion_code", appliedPromotionCode);
        }

        // Add coins usage information to booking request (apply to first leg)
        if (usedCoinAmount > 0) {
            departRequest.put("used_coins_amount", usedCoinAmount);
            Log.d(TAG, "🪙 Adding coins to depart booking request: used_coins_amount=" + usedCoinAmount);
        }

        // Determine selected payment method in one expression so a final local variable
        // is assigned exactly once (avoids compiler error about possible double assignment
        // when using try/catch + assignments inside the try block).
        final String selectedPaymentMethod = (rbQrPayment != null && rbQrPayment.isChecked()) ? "qr"
                : (rbCreditCard != null && rbCreditCard.isChecked()) ? "card" : "offline";

        departRequest.put("payment_method", selectedPaymentMethod);

        Log.d(TAG, "Creating depart booking with payment_method: " + selectedPaymentMethod);

        // If there's no return trip, behave as before
        if (returnTrip == null || returnSeatLabels == null || returnSeatLabels.isEmpty()) {
            // Single-leg flow
            apiService.createBooking(departRequest).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    setLoadingState(false);

                    if (response.isSuccessful() && response.body() != null) {
                        Object bookingIdsObj = response.body().get("booking_ids");
                        ArrayList<Integer> newBookingIds = new ArrayList<>();

                        if (bookingIdsObj instanceof List) {
                            for (Object id : (List<?>) bookingIdsObj) {
                                if (id instanceof Number) {
                                    newBookingIds.add(((Number) id).intValue());
                                } else if (id instanceof String) {
                                    try {
                                        newBookingIds.add(Integer.parseInt((String) id));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }

                        if (!newBookingIds.isEmpty()) {
                            bookingIds = newBookingIds;
                            Log.d(TAG, "Created bookings: " + bookingIds);

                            // Extract total_amount from response and save it
                            try {
                                Object totalAmountObj = response.body().get("total_amount");
                                if (totalAmountObj instanceof Number) {
                                    bookingTotalAmount = ((Number) totalAmountObj).doubleValue();
                                } else if (totalAmountObj instanceof String) {
                                    bookingTotalAmount = Double.parseDouble((String) totalAmountObj);
                                }
                                Log.d(TAG, "Total amount from booking: " + bookingTotalAmount);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing total_amount", e);
                                bookingTotalAmount = 0.0;
                            }

                            // proceed to payment flow
                            isPendingPayment = true;
                            handlePostBookingFlow(selectedPaymentMethod);
                        } else {
                            handlePaymentError("Không thể tạo đặt vé. Vui lòng thử lại.");
                        }
                    } else {
                        setLoadingState(false);
                        handleCreateBookingError(response);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    setLoadingState(false);
                    handlePaymentError("Lỗi kết nối khi tạo đặt vé: " + t.getMessage());
                    Log.e(TAG, "Error creating booking", t);
                }
            });
            return;
        }

        // Round-trip flow: create depart then return booking sequentially
        apiService.createBooking(departRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!(response.isSuccessful() && response.body() != null)) {
                    setLoadingState(false);
                    handleCreateBookingError(response);
                    return;
                }

                // collect depart booking ids and amount
                ArrayList<Integer> combinedBookingIds = new ArrayList<>();
                final double[] totalAmountSum = new double[]{0.0};
                try {
                    Object bookingIdsObj = response.body().get("booking_ids");
                    if (bookingIdsObj instanceof List) {
                        for (Object id : (List<?>) bookingIdsObj) {
                            if (id instanceof Number) combinedBookingIds.add(((Number) id).intValue());
                            else if (id instanceof String) try { combinedBookingIds.add(Integer.parseInt((String) id)); } catch (Exception ignored) {}
                        }
                    }
                    Object tot = response.body().get("total_amount");
                    if (tot instanceof Number) totalAmountSum[0] += ((Number) tot).doubleValue();
                    else if (tot instanceof String) try { totalAmountSum[0] += Double.parseDouble((String) tot); } catch (Exception ignored) {}
                } catch (Exception e) { Log.e(TAG, "Error parsing depart booking response", e); }

                // Now create return booking
                Map<String, Object> returnRequest = new HashMap<>();
                if (userId != null) returnRequest.put("user_id", userId);
                returnRequest.put("trip_id", returnTrip.getId());
                returnRequest.put("seat_labels", returnSeatLabels);
                returnRequest.put("passenger_name", fullName);
                returnRequest.put("passenger_phone", phoneNumber);
                returnRequest.put("passenger_email", email != null ? email : "");
                returnRequest.put("pickup_stop_id", returnPickupStopId);
                returnRequest.put("dropoff_stop_id", returnDropoffStopId);

                if (appliedPromotionCode != null && !appliedPromotionCode.isEmpty()) {
                    returnRequest.put("promotion_code", appliedPromotionCode);
                }
                // For simplicity, do not reapply coins to return leg (coins applied to first leg only)
                returnRequest.put("payment_method", selectedPaymentMethod);

                apiService.createBooking(returnRequest).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call2, Response<Map<String, Object>> response2) {
                        setLoadingState(false);
                        if (!(response2.isSuccessful() && response2.body() != null)) {
                            // Return leg failed; show error but keep depart booking created
                            handlePaymentError("Tạo vé chiều về không thành công. Vui lòng liên hệ hỗ trợ.");
                            // Still populate whatever we have
                            bookingIds = combinedBookingIds;
                            bookingTotalAmount = totalAmountSum[0];
                            isPendingPayment = true;
                            return;
                        }

                        try {
                            Object bookingIdsObj2 = response2.body().get("booking_ids");
                            if (bookingIdsObj2 instanceof List) {
                                for (Object id : (List<?>) bookingIdsObj2) {
                                    if (id instanceof Number) combinedBookingIds.add(((Number) id).intValue());
                                    else if (id instanceof String) try { combinedBookingIds.add(Integer.parseInt((String) id)); } catch (Exception ignored) {}
                                }
                            }
                            Object tot2 = response2.body().get("total_amount");
                            if (tot2 instanceof Number) totalAmountSum[0] += ((Number) tot2).doubleValue();
                            else if (tot2 instanceof String) try { totalAmountSum[0] += Double.parseDouble((String) tot2); } catch (Exception ignored) {}
                        } catch (Exception e) { Log.e(TAG, "Error parsing return booking response", e); }

                        if (!combinedBookingIds.isEmpty()) {
                            bookingIds = combinedBookingIds;
                            bookingTotalAmount = totalAmountSum[0];
                            Log.d(TAG, "Created round-trip bookings: " + bookingIds + ", totalAmount=" + bookingTotalAmount);
                            isPendingPayment = true;
                            handlePostBookingFlow(selectedPaymentMethod);
                        } else {
                            handlePaymentError("Không thể tạo đặt vé khứ hồi. Vui lòng thử lại.");
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call2, Throwable t) {
                        setLoadingState(false);
                        handlePaymentError("Lỗi kết nối khi tạo vé chiều về: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                handlePaymentError("Lỗi kết nối khi tạo vé: " + t.getMessage());
                Log.e(TAG, "Error creating depart booking", t);
            }
        });
    }

    // Sequentially change payment method for all bookingIds starting from index
    private void changePaymentMethodForBookings(int index, String newMethod, Runnable onComplete) {
        if (bookingIds == null || index >= bookingIds.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Integer bookingId = bookingIds.get(index);
        Map<String, String> body = new HashMap<>();
        body.put("payment_method", newMethod);
        apiService.changePaymentMethod(bookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // proceed to next
                    changePaymentMethodForBookings(index + 1, newMethod, onComplete);
                } else {
                    setLoadingState(false);
                    handlePaymentError("Không thể thay đổi phương thức thanh toán: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                handlePaymentError("Lỗi kết nối khi thay đổi phương thức: " + t.getMessage());
            }
        });
    }

    private void handleCreateBookingError(Response<Map<String, Object>> response) {
        setLoadingState(false);
        String err = "Lỗi tạo vé";
        try (okhttp3.ResponseBody eb = response.errorBody()) {
            if (eb != null) err += ": " + eb.string();
        } catch (Exception ignored) {}
        handlePaymentError(err);
    }

    private void navigateToMyBookings() {
        try {
            Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to MyBookingsActivity", e);
        }
    }

    // Handle what happens after bookings are created: process payment based on selected method
    private void handlePostBookingFlow(String paymentMethod) {
        try {
            if (btnConfirmPayment != null) btnConfirmPayment.setEnabled(false);

            if ("qr".equalsIgnoreCase(paymentMethod) || "vnpay".equalsIgnoreCase(paymentMethod) || "payos".equalsIgnoreCase(paymentMethod)) {
                processPayosPayment(bookingIds);
                return;
            }

            if ("card".equalsIgnoreCase(paymentMethod)) {
                confirmNextPayment(bookingIds, 0, "card");
                return;
            }

            // Default: offline/cash
            Toast.makeText(PaymentActivity.this,
                    "Vé đã được đặt với thanh toán tại nhà xe. Vui lòng thanh toán trước khi lên xe.",
                    Toast.LENGTH_LONG).show();

            // Deduct coins applied to first leg (if any)
            if (usedCoinAmount > 0 && bookingIds != null && !bookingIds.isEmpty()) {
                int bookingId = bookingIds.get(0);
                deductUserCoins(bookingId, this::navigateToMyBookings);
            } else {
                navigateToMyBookings();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handlePostBookingFlow", e);
            handlePaymentError("Đã có lỗi trong quá trình xử lý sau khi tạo vé.");
        }
    }
}

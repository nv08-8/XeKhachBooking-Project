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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody; // added for safe response body handling
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PaymentRequest;
import vn.hcmute.busbooking.model.PaymentResponse;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;
import vn.hcmute.busbooking.util.CurrencyUtil;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String PREFS_NAME = "payment_timers";
    private static final long HOLD_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    private TextView tvBusOperator, tvBusType, tvAppName, tvPickup, tvDropoff, tvDate, tvSeat, tvDepartureTime, tvArrivalTime;
    private TextView tvOrigin, tvDestination;
    private TextView tvCountdown;
    private TextView tvPassengerName, tvPassengerPhone;
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
        // tvDate view doesn't exist in layout, set to null to avoid crash
        tvDate = null; // findViewById(R.id.tvDate);
        // seat textview id in layout is tvSeatNumber
        tvSeat = findViewById(R.id.tvSeatNumber);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        // phone field id in layout is tvPhoneNumber
        tvPassengerPhone = findViewById(R.id.tvPhoneNumber);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        // tvArrivalTime view doesn't exist in layout, set to null to avoid crash
        tvArrivalTime = null; // findViewById(R.id.tvArrivalTime);
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
    }

    private boolean collectIntentData() {
         Intent intent = getIntent();
         isPendingPayment = intent.getBooleanExtra("is_pending_payment", false);

         if (isPendingPayment) {
             bookingIds = intent.getIntegerArrayListExtra("booking_ids");
         }

         trip = intent.getParcelableExtra("trip");
         seatLabels = intent.getStringArrayListExtra("seat_labels");

         pickupStopId = intent.getIntExtra("pickup_stop_id", -1);
         dropoffStopId = intent.getIntExtra("dropoff_stop_id", -1);
         pickupStopName = intent.getStringExtra("pickup_stop_name");
         dropoffStopName = intent.getStringExtra("dropoff_stop_name");

         // If this is a pending payment flow we allow missing trip/seatLabels because we'll fetch booking details from server
         if (!isPendingPayment) {
             if (trip == null || seatLabels == null || seatLabels.isEmpty() || pickupStopId == -1 || dropoffStopId == -1 || pickupStopName == null || dropoffStopName == null) {
                 Log.e(TAG, "Missing critical booking data from Intent.");
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
        if (trip != null) {
            tvBusOperator.setText(trip.getOperator());
            tvBusType.setText(trip.getBusType());
            if (tvOrigin != null) tvOrigin.setText(trip.getOrigin());
            if (tvDestination != null) tvDestination.setText(trip.getDestination());
            tvDepartureTime.setText(formatDisplayDateTime(trip.getDepartureTime()));
        }

        if (tvAppName != null) tvAppName.setText(R.string.logo_default);
        // Split pickupStopName (may be "Name - Address") into name and address
        if (tvPickup != null) {
            String name = pickupStopName != null ? pickupStopName : "";
            if (name.contains(" - ")) {
                String[] parts = name.split(" - ", 2);
                tvPickup.setText(parts[0]);
                Object tag = tvPickup.getTag();
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
                tvPickup.setText(name);
                Object tag = tvPickup.getTag();
                if (tag instanceof TextView) {
                    ((TextView) tag).setVisibility(View.GONE);
                }
            }
        }

        if (tvDropoff != null) {
            String name = dropoffStopName != null ? dropoffStopName : "";
            if (name.contains(" - ")) {
                String[] parts = name.split(" - ", 2);
                tvDropoff.setText(parts[0]);
                Object tag = tvDropoff.getTag();
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
                tvDropoff.setText(name);
                Object tag = tvDropoff.getTag();
                if (tag instanceof TextView) {
                    ((TextView) tag).setVisibility(View.GONE);
                }
            }
        }

        String seatText = (seatLabels != null) ? TextUtils.join(", ", seatLabels) : "";
        tvSeat.setText(seatText);

        if (fullName != null && !fullName.isEmpty()) {
            tvPassengerName.setText(fullName);
        } else if (sessionManager.getUserName() != null) {
            tvPassengerName.setText(sessionManager.getUserName());
        }

        String phoneToShow = (phoneNumber != null) ? phoneNumber : sessionManager.getUserPhone();
        if (phoneToShow != null) {
            tvPassengerPhone.setText(maskPhone(phoneToShow));
        }

        updatePricingUI();
    }

    private void updatePricingUI() {
        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip != null ? trip.getPrice() * (seatLabels != null ? seatLabels.size() : 0) : 0);
        tvSubtotal.setText(CurrencyUtil.formatVND(subtotal));

        double totalToShow = subtotal - appliedDiscount;
        if (totalToShow < 0) totalToShow = 0;
        tvTotal.setText(CurrencyUtil.formatVND(totalToShow));
        tvBottomTotal.setText(CurrencyUtil.formatVND(totalToShow));
        btnConfirmPayment.setText(R.string.title_payment);
    }
    
    private String formatDisplayDate(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
            if (date == null) return "";
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + isoDate, e);
            return "";
        }
    }

    private String formatDisplayTime(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
            if (date == null) return "";
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + isoDate, e);
            return "";
        }
    }

    private String formatDisplayDateTime(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
            if (date == null) return "";
            // Format: "16:00 - Thứ Năm, 01/01/2026"
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            return timeFormat.format(date) + " - " + dateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing datetime: " + isoDate, e);
            return "";
        }
    }

    // Mask phone number for display: keep first 3 and last 3 digits, replace middle with ***
    private String maskPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 6) return phone;
        String start = digits.substring(0, 3);
        String end = digits.substring(digits.length() - 3);
        return start + "***" + end;
    }

    /**
     * Normalize payment method to simplified categories: "online", "offline" or "unknown".
     */
    private String normalizePaymentMethod(String method) {
        if (method == null) return "unknown";
        String lower = method.toLowerCase(Locale.getDefault());
        // Online
        if (lower.contains("qr") || lower.contains("payos") || lower.contains("card") || lower.contains("credit") || lower.contains("momo") || lower.contains("vnpay") || lower.contains("online")) {
            return "online";
        }
        // Offline
        if (lower.contains("cash") || lower.contains("offline") || lower.contains("cod") || lower.contains("counter")) {
            return "offline";
        }
        return "unknown";
    }

    private void createBookingAndProcessPayment() {
        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, getString(R.string.msg_booking_error, "Vui lòng đăng nhập để đặt vé"), Toast.LENGTH_SHORT).show();
            return;
        }

        if (getSelectedPaymentMethodId() == -1) {
            Toast.makeText(this, getString(R.string.msg_booking_error, "Vui lòng chọn phương thức thanh toán"), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("trip_id", trip.getId());
        body.put("seat_labels", seatLabels);
        body.put("pickup_stop_id", pickupStopId);
        body.put("dropoff_stop_id", dropoffStopId);

        // ✅ Include passenger_info object (and keep legacy fields for backward compatibility)
        Map<String, String> passengerInfo = new HashMap<>();
        passengerInfo.put("name", fullName != null ? fullName : "");
        passengerInfo.put("phone", phoneNumber != null ? phoneNumber : "");
        passengerInfo.put("email", email != null ? email : "");
        body.put("passenger_info", passengerInfo);
        // Legacy top-level fields (kept for backward compatibility)
        body.put("passenger_name", fullName != null ? fullName : "");
        body.put("passenger_phone", phoneNumber != null ? phoneNumber : "");
        body.put("passenger_email", email != null ? email : "");

        // ✅ Include payment method so backend knows what payment type user selected
        int selectedMethod = getSelectedPaymentMethodId();
        String paymentMethod = "offline"; // default
        if (selectedMethod == R.id.rbQrPayment) {
            paymentMethod = "qr";
        } else if (selectedMethod == R.id.rbCreditCard) {
            paymentMethod = "card";
        } else if (selectedMethod == R.id.rbPayAtOffice) {
            paymentMethod = "offline";
        }
        body.put("payment_method", paymentMethod);
        Log.d(TAG, "Creating booking with payment_method: " + paymentMethod);

        // include applied promotion_code if any
        if (appliedPromotionCode != null && !appliedPromotionCode.isEmpty()) {
            body.put("promotion_code", appliedPromotionCode);
        }

        apiService.createBooking(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object bookingIdsObj = response.body().get("booking_ids");
                    if (bookingIdsObj instanceof List) {
                        List<Integer> newBookingIds = new ArrayList<>();
                        for (Object id : (List<?>) bookingIdsObj) {
                            if (id instanceof Number) {
                                newBookingIds.add(((Number) id).intValue());
                            } else if (id instanceof String) {
                                try {
                                    newBookingIds.add(Integer.parseInt((String) id));
                                } catch (NumberFormatException e) {
                                    handlePaymentError("Lỗi định dạng mã đặt vé.");
                                    return;
                                }
                            }
                        }
                        if (!newBookingIds.isEmpty()) {
                            // Decide behavior based on selected payment method
                            int selected = getSelectedPaymentMethodId();
                            if (selected == R.id.rbQrPayment) {
                                // QR/e-wallet: start local countdown and kick off external payment
                                bookingIds = new ArrayList<>(newBookingIds);
                                isPendingPayment = true;
                                long primary = bookingIds.get(0);
                                long expiry = System.currentTimeMillis() + HOLD_DURATION_MS;
                                saveExpiryForBooking(primary, expiry);
                                if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                                try { NestedScrollView nsv = findViewById(R.id.nestedScrollView); if (nsv != null) nsv.post(() -> nsv.smoothScrollTo(0, cardCountdown.getTop())); } catch (Exception ignored) {}
                                startCountdown(HOLD_DURATION_MS);
                                processPayment(newBookingIds);
                            } else if (selected == R.id.rbCreditCard) {
                                // Credit card: record payment immediately (confirm on server) - no countdown
                                bookingIds = new ArrayList<>(newBookingIds);
                                isPendingPayment = false;
                                // Call confirmPayment for each booking id
                                confirmNextPayment(newBookingIds, 0, "card");
                            } else {
                                // Pay at office: ✅ MUST call confirmNextPayment to properly set status
                                bookingIds = new ArrayList<>(newBookingIds);
                                isPendingPayment = false;
                                // ✅ Call confirmPayment for each booking with "offline" method
                                confirmNextPayment(newBookingIds, 0, "offline");
                            }
                         } else {
                             handlePaymentError("Không nhận được mã đặt vé.");
                         }
                    } else {
                        handlePaymentError("Lỗi phản hồi từ máy chủ.");
                    }
                } else {
                    String errorMessage = "Không thể tạo đặt vé.";
                    if (response.errorBody() != null) {
                        try (ResponseBody rb = response.errorBody()) {
                            String errorString = rb.string();
                            JSONObject errorJson = new JSONObject(errorString);
                            errorMessage = errorJson.optString("message", errorMessage);

                            // More specific check for seat availability error
                            String lowerCaseError = errorMessage.toLowerCase();
                            if (lowerCaseError.contains("seat") && lowerCaseError.contains("not available")) {
                                showSeatNotAvailableDialog();
                                return; // Stop further processing
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body", e);
                        }
                    }
                    handlePaymentError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handlePaymentError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void showSeatNotAvailableDialog() {
        setLoadingState(false);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.seat_unavailable_title))
                .setMessage(getString(R.string.seat_unavailable_message))
                .setPositiveButton(getString(R.string.select_seats), (dialog, which) -> {
                     // Go back to SeatSelectionActivity
                     Intent intent = new Intent(PaymentActivity.this, SeatSelectionActivity.class);
                     intent.putExtra("trip", trip);
                     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                     startActivity(intent);
                     finish();
                 })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void processPayment(List<Integer> ids) {
        int selectedId = getSelectedPaymentMethodId();
        if (selectedId == R.id.rbQrPayment) {
            processPayosPayment(ids);
        } else if (selectedId == R.id.rbCreditCard) {
            handlePaymentError("Thanh toán bằng thẻ tín dụng chưa được hỗ trợ.");
        } else {
            processCashPayment(ids);
        }
    }

    private void processPayosPayment(List<Integer> ids) {
        setLoadingState(true);

        // ✅ Validate required data before creating payment
        if (trip == null) {
            setLoadingState(false);
            handlePaymentError("Thiếu thông tin chuyến xe. Vui lòng thoát ra và vào lại màn hình này.");
            Log.e(TAG, "❌ processPayosPayment: trip is NULL!");
            return;
        }

        if (seatLabels == null || seatLabels.isEmpty()) {
            setLoadingState(false);
            handlePaymentError("Thiếu thông tin ghế. Vui lòng thoát ra và vào lại màn hình này.");
            Log.e(TAG, "❌ processPayosPayment: seatLabels is NULL or empty!");
            return;
        }

        // Use default values if passenger info is missing
        String paymentFullName = (fullName != null && !fullName.isEmpty()) ? fullName : "Khách hàng";
        String paymentEmail = (email != null && !email.isEmpty()) ? email : "customer@example.com";
        String paymentPhone = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "0000000000";

        String orderId = String.valueOf(System.currentTimeMillis());

        // ✅ Calculate amount with fallback
        double pricePerSeat = trip.getPrice();
        if (pricePerSeat <= 0 && bookingTotalAmount != null && bookingTotalAmount > 0) {
            // Fallback: use total_amount from booking divided by seat count
            pricePerSeat = bookingTotalAmount / seatLabels.size();
            Log.d(TAG, "Using price from bookingTotalAmount: " + pricePerSeat);
        }

        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip.getPrice() * seatLabels.size());
        int finalAmount = (int) (subtotal - appliedDiscount);
        if (finalAmount < 2000) finalAmount = 2000; // PayOS minimum

        Log.d(TAG, "Creating PayOS payment: orderId=" + orderId + ", amount=" + finalAmount +
                   ", bookingIds=" + ids + ", name=" + paymentFullName +
                   ", seatCount=" + seatLabels.size());

        PaymentRequest request = new PaymentRequest(orderId, finalAmount, ids, paymentFullName, paymentEmail, paymentPhone);

        apiService.createPayosPayment(request).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String checkoutUrl = response.body().getCheckoutUrl();
                    if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                        startActivity(browserIntent);
                        setLoadingState(false);
                        // Start local timer to remind user to complete payment when coming back
                        if (ids != null && !ids.isEmpty()) {
                            long primary = ids.get(0);
                            long expiry = System.currentTimeMillis() + HOLD_DURATION_MS;
                            saveExpiryForBooking(primary, expiry);
                        }
                    } else {
                        handlePaymentError("Không nhận được link thanh toán.");
                    }
                } else {
                    handlePaymentError("Không thể tạo link thanh toán.");
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                handlePaymentError("Lỗi mạng khi tạo link thanh toán: " + t.getMessage());
            }
        });
    }

    private void confirmNextPayment(List<Integer> ids, int index, String paymentMethod) {
        if (index >= ids.size()) {
            onAllPaymentsSuccess(ids.get(0));
            return;
        }

        setLoadingState(true);
        int currentBookingId = ids.get(index);
        Map<String, String> body = new HashMap<>();
        body.put("payment_method", paymentMethod);

        // If card payment, attach card details (temporary: raw PAN sent to server) - do NOT store CVV on server
        if ("card".equalsIgnoreCase(paymentMethod)) {
            try {
                String pan = etCardNumber != null && etCardNumber.getText() != null ? etCardNumber.getText().toString().replaceAll("\\s+", "") : "";
                String expiry = etExpiryDate != null && etExpiryDate.getText() != null ? etExpiryDate.getText().toString().trim() : "";
                String cvv = etCvv != null && etCvv.getText() != null ? etCvv.getText().toString().trim() : "";
                if (!pan.isEmpty()) body.put("card_number", pan);
                if (!expiry.isEmpty()) body.put("expiry", expiry);
                if (!cvv.isEmpty()) body.put("cvv", cvv);
                if (fullName != null && !fullName.isEmpty()) body.put("card_holder_name", fullName);
            } catch (Exception ignored) {}
        }

        apiService.confirmPayment(currentBookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    confirmNextPayment(ids, index + 1, paymentMethod);
                } else {
                    // Treat non-success response as optimistic success for UX: continue to next booking
                    Log.w(TAG, "confirmPayment returned code=" + response.code() + ", treating as success for UX");
                    confirmNextPayment(ids, index + 1, paymentMethod);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // On network failure, treat as optimistic success (per requested behavior)
                Log.e(TAG, "confirmPayment network failure, treating as optimistic success", t);
                confirmNextPayment(ids, index + 1, paymentMethod);
            }
        });
    }

    // Luhn algorithm
    private boolean luhnCheck(String ccNumber) {
        if (ccNumber == null) return false;
        String s = ccNumber.replaceAll("\\D", "");
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

    // expiry format: MM/YY or MM/YYYY or MM-YY etc.
    private boolean isExpiryValid(String expiry) {
        if (expiry == null) return false;
        expiry = expiry.replaceAll("\\s", "");
        String[] parts = null;
        if (expiry.contains("/")) parts = expiry.split("/");
        else if (expiry.contains("-")) parts = expiry.split("-");
        else if (expiry.length() == 4) { // MMyy
            parts = new String[]{expiry.substring(0,2), expiry.substring(2)};
        }
        if (parts == null || parts.length < 2) return false;
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) return false;
            if (year < 100) year += 2000;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setLenient(false);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.MONTH, month - 1);
            cal.set(java.util.Calendar.YEAR, year);
            // expiry is end of month
            java.util.Calendar end = (java.util.Calendar) cal.clone();
            end.add(java.util.Calendar.MONTH, 1);
            end.add(java.util.Calendar.DAY_OF_MONTH, -1);
            // compare end of expiry month to now
            return end.getTimeInMillis() > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCvvValid(String cvv) {
        if (cvv == null) return false;
        if (!cvv.matches("\\d{3,4}")) return false;
        return true;
    }

    // Helper: returns selected payment RadioButton id or -1
    private int getSelectedPaymentMethodId() {
        try {
            if (rbQrPayment != null && rbQrPayment.isChecked()) return R.id.rbQrPayment;
            if (rbCreditCard != null && rbCreditCard.isChecked()) return R.id.rbCreditCard;
            if (rbPayAtOffice != null && rbPayAtOffice.isChecked()) return R.id.rbPayAtOffice;
        } catch (Exception ignored) {}
        return -1;
    }

    // Change payment method for a list of bookings sequentially
    private void changePaymentMethodForBookings(int index, String newPaymentMethod, Runnable onComplete) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            handlePaymentError("Không có mã đặt vé để đổi phương thức thanh toán.");
            return;
        }
        if (index >= bookingIds.size()) {
            // done
            setLoadingState(false);
            if (onComplete != null) onComplete.run();
            return;
        }
        int id = bookingIds.get(index);
        Map<String, String> body = new HashMap<>();
        body.put("payment_method", newPaymentMethod);
        apiService.changePaymentMethod(id, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // proceed to next
                    changePaymentMethodForBookings(index + 1, newPaymentMethod, onComplete);
                } else {
                    handlePaymentError("Không thể đổi phương thức thanh toán.");
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handlePaymentError("Lỗi kết nối khi đổi phương thức thanh toán.");
            }
        });
    }

    private void changePaymentMethodToOfflineConfirm(String newPaymentMethod) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đổi phương thức")
                .setMessage("Bạn có chắc chắn muốn đổi sang thanh toán tại nhà xe không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> changePaymentMethodToOffline())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void changePaymentMethodToOffline() {
        if (bookingIds == null || bookingIds.isEmpty()) {
            handlePaymentError("Không có mã đặt vé để đổi phương thức.");
            return;
        }
        setLoadingState(true);
        changePaymentMethodForBookings(0, "offline", () -> {
            setLoadingState(false);
            Toast.makeText(PaymentActivity.this, "Đã đổi sang thanh toán tại nhà xe", Toast.LENGTH_SHORT).show();
        });
    }

    private void setLoadingState(boolean isLoading) {
        try {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (btnConfirmPayment != null) btnConfirmPayment.setEnabled(!isLoading);
        } catch (Exception ignored) {}
    }

    private void handlePaymentError(String msg) {
        setLoadingState(false);
        Log.e(TAG, "Payment error: " + msg);
        try { Toast.makeText(PaymentActivity.this, msg, Toast.LENGTH_LONG).show(); } catch (Exception ignored) {}
    }

    private void saveExpiryForBooking(long bookingId, long expiryMillis) {
        try {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putLong("expiry_" + bookingId, expiryMillis).apply();
        } catch (Exception ignored) {}
    }

    private void onAllPaymentsSuccess(int primaryBookingId) {
        setLoadingState(false);
        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
        // Open MyBookingsActivity (user preference) instead of BookingDetailActivity to avoid detail-page reload race
        try {
            Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
            // Open booking list so user sees their confirmed tickets
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } catch (Exception ignored) {}
        finish();
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
                // call validation API to apply
                validateAndApplyPromo(code);
            });

            if (tvSelectPromo != null) {
                tvSelectPromo.setOnClickListener(v -> showPromotionsBottomSheet());
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

    private boolean validateCardInputs() {
        if (etCardNumber == null || etExpiryDate == null || etCvv == null) return false;
        String rawPan = etCardNumber.getText() == null ? "" : etCardNumber.getText().toString();
        String pan = rawPan.replaceAll("[^0-9]", "");
        String expiry = etExpiryDate.getText() == null ? "" : etExpiryDate.getText().toString().trim();
        String cvv = etCvv.getText() == null ? "" : etCvv.getText().toString().trim();

        // Clear previous errors
        try { etCardNumber.setError(null); etExpiryDate.setError(null); etCvv.setError(null); } catch (Exception ignored) {}

        if (pan.isEmpty() || pan.length() < 16 || pan.length() > 19) {
            try { etCardNumber.requestFocus(); etCardNumber.setError("Số thẻ phải có ít nhất 16 chữ số"); } catch (Exception ignored) {}
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
                try { if (title != null) title.setText(p.getCode() != null ? p.getCode() : p.getTitle());
                      if (desc != null) desc.setText(p.getDescription() != null ? p.getDescription() : ""); } catch (Exception ignored) {}
                holder.itemView.setOnClickListener(v -> {
                    try {
                        String code = p.getCode() != null ? p.getCode() : (p.getTitle() != null ? p.getTitle() : "");
                        if (etPromoCode != null) etPromoCode.setText(code);
                        validateAndApplyPromo(code);
                    } catch (Exception e) { Log.e(TAG, "Error on promo item click", e); }
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
}

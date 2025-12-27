package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.PaymentRequest;
import vn.hcmute.busbooking.model.PaymentResponse;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String PREFS_NAME = "payment_timers";
    private static final long HOLD_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    private TextView tvBusOperator, tvBusType, tvPickup, tvDropoff, tvDate, tvSeat, tvDepartureTime, tvArrivalTime;
    private TextView tvCountdown;
    private TextView tvBottomTotal;
    private Button btnConfirmPayment;
    private com.google.android.material.button.MaterialButton btnChangeToOffline;
    private ProgressBar progressBar;

    private MaterialCardView cardCreditCard, cardQrPayment, cardPayAtOffice;
    private RadioButton rbCreditCard, rbQrPayment, rbPayAtOffice;
    private LinearLayout creditCardForm;

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
    private long millisUntilFinished = 0L;
    private MaterialCardView cardCountdown;

    // Promo fields
    private EditText etPromoCode;
    private com.google.android.material.button.MaterialButton btnApplyPromo;
    private TextView tvPromoDetails, tvDiscountApplied, tvSubtotal, tvTotal;
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
        tvPickup = findViewById(R.id.tvPickup);
        tvDropoff = findViewById(R.id.tvDropoff);
        tvDate = findViewById(R.id.tvDate);
        tvSeat = findViewById(R.id.tvSeat);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
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

        // Promo-related views
        etPromoCode = findViewById(R.id.etPromoCode);
        btnApplyPromo = findViewById(R.id.btnApplyPromo);
        tvPromoDetails = findViewById(R.id.tvPromoDetails);
        tvDiscountApplied = findViewById(R.id.tvDiscountApplied);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
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

        fullName = intent.getStringExtra("fullName");
        phoneNumber = intent.getStringExtra("phoneNumber");
        email = intent.getStringExtra("email");

        if (fullName == null) fullName = sessionManager.getUserName();
        if (phoneNumber == null) phoneNumber = sessionManager.getUserPhone();
        if (email == null) email = sessionManager.getUserEmail();
        
        return true;
    }

    private void populateBookingSummary() {
        tvBusOperator.setText(trip.getOperator());
        tvBusType.setText(trip.getBusType());
        tvPickup.setText(pickupStopName);
        tvDropoff.setText(dropoffStopName);
        tvDate.setText(formatDisplayDate(trip.getDepartureTime()));
        tvDepartureTime.setText(formatDisplayTime(trip.getDepartureTime()));
        tvArrivalTime.setText(formatDisplayTime(trip.getArrivalTime()));

        String seatText = TextUtils.join(", ", seatLabels);
        tvSeat.setText("Ghế: " + seatText);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip.getPrice() * seatLabels.size());
        tvSubtotal.setText(formatter.format(subtotal));

        double totalToShow = subtotal - appliedDiscount; // appliedDiscount is 0 if none
        if (totalToShow < 0) totalToShow = 0;
        tvTotal.setText(formatter.format(totalToShow));

        // Update bottom bar total and keep button text simple
        tvBottomTotal.setText(formatter.format(totalToShow));
        btnConfirmPayment.setText(R.string.title_payment);
    }
    
    private String formatDisplayDate(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
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
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + isoDate, e);
            return "";
        }
    }

    private void setupPaymentMethodSelection() {
        View.OnClickListener listener = v -> {
            // ✅ Removed isPendingPayment check - allow users to change payment methods
            // The enable/disable logic is handled in fetchBookingDetailsAndInit()

            int id = v.getId();
            if (id == R.id.cardCreditCard || id == R.id.rbCreditCard) {
                selectPaymentMethod(R.id.rbCreditCard);
            } else if (id == R.id.cardQrPayment || id == R.id.rbQrPayment) {
                selectPaymentMethod(R.id.rbQrPayment);
            } else if (id == R.id.cardPayAtOffice || id == R.id.rbPayAtOffice) {
                selectPaymentMethod(R.id.rbPayAtOffice);
            }
        };

        cardCreditCard.setOnClickListener(listener);
        cardQrPayment.setOnClickListener(listener);
        cardPayAtOffice.setOnClickListener(listener);
        rbCreditCard.setOnClickListener(listener);
        rbQrPayment.setOnClickListener(listener);
        rbPayAtOffice.setOnClickListener(listener);

        if (!isPendingPayment) {
            selectPaymentMethod(R.id.rbCreditCard);
        }
    }

    private void selectPaymentMethod(int selectedId) {
        rbCreditCard.setChecked(selectedId == R.id.rbCreditCard);
        rbQrPayment.setChecked(selectedId == R.id.rbQrPayment);
        rbPayAtOffice.setChecked(selectedId == R.id.rbPayAtOffice);

        creditCardForm.setVisibility(rbCreditCard.isChecked() ? View.VISIBLE : View.GONE);

        updateCardAppearance(cardCreditCard, rbCreditCard.isChecked());
        updateCardAppearance(cardQrPayment, rbQrPayment.isChecked());
        updateCardAppearance(cardPayAtOffice, rbPayAtOffice.isChecked());
    }

    private void updateCardAppearance(MaterialCardView card, boolean isSelected) {
        if (isSelected) {
            card.setStrokeWidth(2);
            card.setStrokeColor(ContextCompat.getColor(this, R.color.colorPrimary));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.backgroundLight));
        } else {
            card.setStrokeWidth(0);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private int getSelectedPaymentMethodId() {
        if (rbCreditCard.isChecked()) return R.id.rbCreditCard;
        if (rbQrPayment.isChecked()) return R.id.rbQrPayment;
        if (rbPayAtOffice.isChecked()) return R.id.rbPayAtOffice;
        return -1;
    }

    /**
     * Normalize payment method strings for consistent comparison
     * Maps various payment method names to either "online" or "offline"
     * @param method The payment method string from UI or API
     * @return "online" for QR/Card payments, "offline" for cash payments
     */
    private String normalizePaymentMethod(String method) {
        if (method == null) return "unknown";

        String lower = method.toLowerCase(Locale.getDefault());

        // Online payment methods
        if (lower.contains("qr") || lower.contains("payos") ||
            lower.contains("card") || lower.contains("credit") ||
            lower.contains("momo") || lower.contains("vnpay") ||
            lower.equals("online")) {
            return "online";
        }

        // Offline payment methods
        if (lower.contains("cash") || lower.contains("offline") ||
            lower.contains("cod") || lower.contains("counter") ||
            lower.equals("offline")) {
            return "offline";
        }

        return lower; // Return as-is if can't classify
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

        // ✅ Include passenger info
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
                                // Pay at office or others: pending but NO countdown
                                bookingIds = new ArrayList<>(newBookingIds);
                                isPendingPayment = false;
                                // Direct user to MyBookings with notice
                                setLoadingState(false);
                                Toast.makeText(PaymentActivity.this, getString(R.string.msg_booking_error, "Đặt vé lưu thành công. Thanh toán tại nhà xe."), Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                                finish();
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
                        try {
                            String errorString = response.errorBody().string();
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
            handlePaymentError("Thiếu thông tin chuyến xe. Vui lòng thử lại.");
            return;
        }

        if (seatLabels == null || seatLabels.isEmpty()) {
            setLoadingState(false);
            handlePaymentError("Thiếu thông tin ghế. Vui lòng thử lại.");
            return;
        }

        // Use default values if passenger info is missing
        String paymentFullName = (fullName != null && !fullName.isEmpty()) ? fullName : "Khách hàng";
        String paymentEmail = (email != null && !email.isEmpty()) ? email : "customer@example.com";
        String paymentPhone = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "0000000000";

        String orderId = String.valueOf(System.currentTimeMillis());
        int amount = (int) (trip.getPrice() * seatLabels.size());

        Log.d(TAG, "Creating PayOS payment: orderId=" + orderId + ", amount=" + amount +
                   ", bookingIds=" + ids + ", name=" + paymentFullName);

        PaymentRequest request = new PaymentRequest(orderId, amount, ids, paymentFullName, paymentEmail, paymentPhone);

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

    private void processCashPayment(List<Integer> ids) {
        confirmNextPayment(ids, 0, "cash");
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

        apiService.confirmPayment(currentBookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    confirmNextPayment(ids, index + 1, paymentMethod);
                } else {
                    handlePaymentError("Xác nhận thanh toán thất bại.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handlePaymentError("Lỗi mạng khi xác nhận thanh toán.");
            }
        });
    }

    private void handlePaymentError(String message) {
        Log.e(TAG, "Payment Error: " + message);
        setLoadingState(false);
        Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void onAllPaymentsSuccess(int primaryBookingId) {
        setLoadingState(false);
        Toast.makeText(PaymentActivity.this, getString(R.string.payment_success), Toast.LENGTH_LONG).show();

        Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnConfirmPayment.setEnabled(!isLoading);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Skip countdown/expiry check for offline payments
        if (isOfflinePayment) {
            Log.d(TAG, "Skipping expiry check - offline payment detected");
            return;
        }

        // Check expiry first - if -1, it's offline payment
        if (isPendingPayment && bookingIds != null && !bookingIds.isEmpty()) {
            long expiry = getExpiryForBooking(bookingIds.get(0));
            if (expiry == -1L) {
                Log.d(TAG, "Expiry is -1, treating as offline payment - SKIP verification");
                isOfflinePayment = true;
                return;
            }
        }

        // If this was a pending payment flow, first verify payment status on server, then resume or start timer
        if (isPendingPayment && bookingIds != null && !bookingIds.isEmpty()) {
            int primaryId = bookingIds.get(0);
            // Call backend to get booking details and check payment status
            apiService.getBookingDetails(primaryId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> body = response.body();

                        // ✅ CHECK PAYMENT METHOD FIRST - Critical fix for offline payment bug
                        String paymentMethodFromServer = null;
                        Object paymentMethodObj = body.get("payment_method");
                        if (paymentMethodObj instanceof String) {
                            paymentMethodFromServer = ((String) paymentMethodObj).toLowerCase(Locale.getDefault());
                        }

                        // If server says it's offline payment, update flag and skip all checks
                        if (paymentMethodFromServer != null &&
                            (paymentMethodFromServer.contains("cash") ||
                             paymentMethodFromServer.contains("offline") ||
                             paymentMethodFromServer.contains("cod") ||
                             paymentMethodFromServer.contains("counter"))) {
                            Log.d(TAG, "Server confirmed offline payment - skipping expiry check");
                            isOfflinePayment = true;
                            currentPaymentMethod = paymentMethodFromServer;
                            // Clear any existing expiry
                            saveExpiryForBooking(primaryId, -1L);
                            // Stop any countdown
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                                countDownTimer = null;
                            }
                            if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                            return; // ← CRITICAL: Don't check expiry for offline payments!
                        }

                        boolean isPaid = false;
                        // Heuristic checks for common keys
                        Object paidObj = body.get("paid");
                        if (paidObj instanceof Boolean) {
                            isPaid = (Boolean) paidObj;
                        } else {
                            Object statusObj = body.get("status");
                            if (statusObj instanceof String) {
                                String s = ((String) statusObj).toLowerCase(Locale.getDefault());
                                if (s.contains("paid") || s.contains("completed") || s.contains("success")) isPaid = true;
                            }
                            Object paymentStatus = body.get("payment_status");
                            if (!isPaid && paymentStatus instanceof String) {
                                String s = ((String) paymentStatus).toLowerCase(Locale.getDefault());
                                if (s.contains("paid") || s.contains("completed") || s.contains("success")) isPaid = true;
                            }
                        }

                        if (isPaid) {
                            // Payment completed on server -> stop any local timer, clear expiry and navigate to MyBookings
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                                countDownTimer = null;
                            }
                            saveExpiryForBooking(primaryId, -1L);
                            setLoadingState(false);
                            // Notify user and go to MyBookings
                            new AlertDialog.Builder(PaymentActivity.this)
                                    .setTitle(getString(R.string.payment_success))
                                    .setMessage(getString(R.string.payment_received_message))
                                    .setPositiveButton(getString(R.string.view_tickets), (dialog, which) -> {
                                        Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                            return;
                        }
                    }
                    // Not paid (or couldn't determine) -> resume countdown using stored expiry
                    // ✅ But only if it's online payment (not offline)
                    long expiry = getExpiryForBooking(primaryId);
                    long now = System.currentTimeMillis();
                    if (expiry <= now) {
                        onHoldExpired();
                    } else {
                        if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                        startCountdown(expiry - now);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    // Network failure verifying payment: fallback to local expiry timer
                    // ✅ But ONLY if we know it's not offline payment
                    if (isOfflinePayment) {
                        Log.d(TAG, "Network error but offline payment flag is set - skip expiry check");
                        return;
                    }

                    long expiry = getExpiryForBooking(primaryId);
                    long now = System.currentTimeMillis();

                    // ✅ Additional safety: if expiry is -1, it means offline payment
                    if (expiry == -1L) {
                        Log.d(TAG, "Expiry is -1, treating as offline payment");
                        isOfflinePayment = true;
                        return;
                    }

                    if (expiry <= now) {
                        onHoldExpired();
                    } else {
                        if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                        startCountdown(expiry - now);
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // Countdown helpers
    private void startCountdown(long millis) {
        millisUntilFinished = millis;
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new android.os.CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long l) {
                millisUntilFinished = l;
                tvCountdown.setText(formatMillis(l));
            }

            @Override
            public void onFinish() {
                millisUntilFinished = 0;
                tvCountdown.setText("00:00");
                onHoldExpired();
            }
         }.start();
        // show a short toast to confirm countdown started
        try { Toast.makeText(this, getString(R.string.hold_started_toast, formatMillis(millis)), Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    private void onHoldExpired() {
        // ✅ CRITICAL SAFETY CHECK: Never auto-cancel offline payments!
        if (isOfflinePayment) {
            Log.d(TAG, "onHoldExpired called but isOfflinePayment=true - SKIP cancellation");
            return;
        }

        // Additional safety: check if expiry is -1 (offline payment marker)
        if (bookingIds != null && !bookingIds.isEmpty()) {
            long expiry = getExpiryForBooking(bookingIds.get(0));
            if (expiry == -1L) {
                Log.d(TAG, "onHoldExpired called but expiry=-1 (offline) - SKIP cancellation");
                isOfflinePayment = true; // Update flag for consistency
                return;
            }
        }

        // Cancel bookings on server for all bookingIds (online payment only)
        if (bookingIds == null || bookingIds.isEmpty()) return;
        setLoadingState(true);
        // cancel each booking, then navigate to MyBookings
        cancelBookingsSequentially(0);
    }

    private void cancelBookingsSequentially(int index) {
        if (index >= bookingIds.size()) {
            setLoadingState(false);
            // clear expiry for primary
            saveExpiryForBooking(bookingIds.get(0), -1L);
            // Show a popup to inform the user that the hold expired and tickets were canceled
            showCancelledDialog();
             return;
         }
         int id = bookingIds.get(index);
         apiService.cancelBooking(id).enqueue(new Callback<Map<String, Object>>() {
             @Override
             public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                 // ignore response details, proceed
                 cancelBookingsSequentially(index + 1);
             }

             @Override
             public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                 // still continue
                 cancelBookingsSequentially(index + 1);
             }
         });
     }

     private void showCancelledDialog() {
        new AlertDialog.Builder(PaymentActivity.this)
                .setTitle("Vé đã bị hủy")
                .setMessage("Vé của bạn đã bị hủy vì không hoàn tất thanh toán trong thời gian quy định. Bạn có thể kiểm tra trạng thái trong mục 'Vé của tôi' (Đã hủy).")
                .setPositiveButton("Xem vé", (dialog, which) -> {
                    Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
                    intent.putExtra("show_cancelled", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    // Persist expiry timestamp per booking id in SharedPreferences
    private void saveExpiryForBooking(long bookingId, long expiryTs) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putLong(String.valueOf(bookingId), expiryTs).apply();
    }

    private long getExpiryForBooking(long bookingId) {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong(String.valueOf(bookingId), -1L);
    }

    private void fetchBookingDetailsAndInit(int bookingId) {
        setLoadingState(true);
        // show countdown placeholder immediately so user sees timer area while we fetch details
        if (cardCountdown != null) {
            cardCountdown.setVisibility(View.VISIBLE);
            if (tvCountdown != null) tvCountdown.setText(formatMillis(HOLD_DURATION_MS));
        }
        // Inform user (small toast) that countdown area is prepared (useful for debugging/QA)
        try { Toast.makeText(this, "Đang tải chi tiết vé... Đếm ngược sẽ hiển thị ngay sau khi tải.", Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
         apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    try {
                        // Build minimal Trip object from response if possible
                        Trip t = new Trip();
                        // trip id may be in trip_id or trip_id column
                        Object tripIdObj = data.get("trip_id");
                        if (tripIdObj instanceof Number) t.setId(((Number) tripIdObj).intValue());
                        if (data.get("operator") instanceof String) t.setOperator((String) data.get("operator"));
                        if (data.get("bus_type") instanceof String) t.setBusType((String) data.get("bus_type"));
                        if (data.get("departure_time") instanceof String) t.setDepartureTime((String) data.get("departure_time"));
                        if (data.get("arrival_time") instanceof String) t.setArrivalTime((String) data.get("arrival_time"));
                        // price: prefer booking total_amount if present
                        if (data.get("total_amount") instanceof Number) {
                            bookingTotalAmount = ((Number) data.get("total_amount")).doubleValue();
                        } else if (data.get("total_amount") instanceof String) {
                            try { bookingTotalAmount = Double.parseDouble((String) data.get("total_amount")); } catch (Exception ignored) {}
                        }

                        trip = t;

                        // seat labels
                        Object seatsObj = data.get("seat_labels");
                        ArrayList<String> seats = new ArrayList<>();
                        if (seatsObj instanceof java.util.List) {
                            for (Object s : (java.util.List<?>) seatsObj) if (s != null) seats.add(String.valueOf(s));
                        } else if (seatsObj instanceof String) {
                            seats.add((String) seatsObj);
                        } else if (data.get("seat_label") instanceof String) {
                            seats.add((String) data.get("seat_label"));
                        }
                        seatLabels = seats;

                        // pickup / dropoff
                        if (data.get("pickup_location") instanceof String) pickupStopName = (String) data.get("pickup_location");
                        if (data.get("dropoff_location") instanceof String) dropoffStopName = (String) data.get("dropoff_location");

                        // ✅ Extract passenger info for payment processing
                        if (data.get("passenger_name") instanceof String) fullName = (String) data.get("passenger_name");
                        if (data.get("passenger_phone") instanceof String) phoneNumber = (String) data.get("passenger_phone");
                        // If email not in booking, try to get from user session
                        if (data.get("passenger_email") instanceof String) {
                            email = (String) data.get("passenger_email");
                        } else if (sessionManager != null && sessionManager.isLoggedIn()) {
                            email = sessionManager.getUserEmail();
                        }

                        // Determine payment method for this booking (if present)
                        String paymentMethodStr = null;
                        if (data.get("payment_method") instanceof String) paymentMethodStr = (String) data.get("payment_method");
                        String statusStr = null;
                        if (data.get("status") instanceof String) statusStr = (String) data.get("status");

                        // Store current payment method for later checks
                        currentPaymentMethod = paymentMethodStr;

                        // Detect if current payment method is online (QR/Card) or offline (Cash)
                        boolean isCurrentlyOnlinePayment = false;
                        boolean isCurrentlyOfflinePayment = false;

                        if (paymentMethodStr != null) {
                            String lower = paymentMethodStr.toLowerCase(Locale.getDefault());
                            if (lower.contains("qr") || lower.contains("payos") || lower.contains("card") ||
                                lower.contains("credit") || lower.contains("momo") || lower.contains("vnpay")) {
                                isCurrentlyOnlinePayment = true;
                            } else if (lower.contains("cash") || lower.contains("offline") ||
                                       lower.contains("cod") || lower.contains("counter")) {
                                isCurrentlyOfflinePayment = true;
                                isOfflinePayment = true; // Set flag for onResume() check
                            }
                        }

                        // Save expiry derived from created_at if not already saved
                        Object createdObj = data.get("created_at");
                        long createdMs = -1L;
                        if (createdObj instanceof String) {
                            try {
                                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                Date d = iso.parse((String) createdObj);
                                if (d != null) createdMs = d.getTime();
                            } catch (Exception e) {
                                // Try without milliseconds
                                try {
                                    SimpleDateFormat iso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                                    iso2.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                    Date d2 = iso2.parse((String) createdObj);
                                    if (d2 != null) createdMs = d2.getTime();
                                } catch (Exception ignored) {
                                    Log.e(TAG, "Failed to parse created_at: " + createdObj);
                                }
                            }
                        }

                        long primary = bookingId;
                        long existingExpiry = getExpiryForBooking(primary);
                        long expiryToUse = existingExpiry;

                        if (existingExpiry == -1L) {
                            // Only calculate expiry if we have valid created_at from server
                            if (createdMs > 0) {
                                expiryToUse = createdMs + HOLD_DURATION_MS;
                                saveExpiryForBooking(primary, expiryToUse);
                                Log.d(TAG, "📅 Calculated expiry from created_at: " + new Date(expiryToUse));
                            } else {
                                Log.e(TAG, "⚠️ Cannot calculate expiry - created_at is invalid!");
                                // Don't save any expiry - let payment stay pending without auto-cancel
                            }
                        } else {
                            Log.d(TAG, "📅 Using existing expiry: " + new Date(existingExpiry));
                        }

                        // Continue initialization
                        populateBookingSummary();
                        setupPaymentMethodSelection();
                        setupPromoHandlers(); // Setup promotion handlers

                        // Setup UI based on current payment method and status
                        if ("pending".equalsIgnoreCase(statusStr) || "expired".equalsIgnoreCase(statusStr)) {
                            isPendingPayment = true;

                            if (isCurrentlyOnlinePayment) {
                                // Currently online payment (QR/Card) - show countdown and allow change to offline
                                selectPaymentMethod(R.id.rbQrPayment);
                                // Allow switching to offline
                                cardPayAtOffice.setClickable(true);
                                rbPayAtOffice.setEnabled(true);
                                // Show countdown and change to offline button
                                if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                                if (btnChangeToOffline != null) btnChangeToOffline.setVisibility(View.VISIBLE);

                                // Start countdown if not expired
                                long primaryId = primary;
                                long expiry = getExpiryForBooking(primaryId);

                                // Use the expiry we just calculated/loaded above
                                if (expiry == -1L && createdMs > 0) {
                                    expiry = createdMs + HOLD_DURATION_MS;
                                    saveExpiryForBooking(primaryId, expiry);
                                }

                                long now = System.currentTimeMillis();

                                if (expiry > 0) {
                                    if ("expired".equalsIgnoreCase(statusStr) || expiry <= now) {
                                        // Show expired message - but ONLY for online payments
                                        if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                                        if (btnChangeToOffline != null) btnChangeToOffline.setVisibility(View.VISIBLE);
                                        Toast.makeText(PaymentActivity.this,
                                            "Vé đã hết hạn thanh toán. Bạn có thể đổi sang thanh toán tại nhà xe.",
                                            Toast.LENGTH_LONG).show();
                                    } else {
                                        long remaining = expiry - now;
                                        Log.d(TAG, "⏱️ Starting countdown: " + (remaining/1000) + " seconds remaining");
                                        startCountdown(remaining);
                                    }
                                } else {
                                    Log.w(TAG, "⚠️ No valid expiry time - countdown not started");
                                }
                            } else if (isCurrentlyOfflinePayment) {
                                // Currently offline payment (Cash) - allow switching to online
                                selectPaymentMethod(R.id.rbPayAtOffice);
                                // Enable all payment methods so user can switch
                                cardCreditCard.setClickable(true);
                                rbCreditCard.setEnabled(true);
                                cardQrPayment.setClickable(true);
                                rbQrPayment.setEnabled(true);
                                cardPayAtOffice.setClickable(true);
                                rbPayAtOffice.setEnabled(true);
                                // Hide countdown
                                if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                                if (btnChangeToOffline != null) btnChangeToOffline.setVisibility(View.GONE);
                                // Clear expiry timestamp to prevent auto-cancellation
                                saveExpiryForBooking(primary, -1L);
                            } else {
                                // Unknown payment method - default to allowing all methods
                                selectPaymentMethod(R.id.rbQrPayment);
                                cardCreditCard.setClickable(true);
                                rbCreditCard.setEnabled(true);
                                cardQrPayment.setClickable(true);
                                rbQrPayment.setEnabled(true);
                                cardPayAtOffice.setClickable(true);
                                rbPayAtOffice.setEnabled(true);
                            }
                        } else {
                            // Not pending/expired - normal flow
                            isPendingPayment = false;
                            if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                            if (btnChangeToOffline != null) btnChangeToOffline.setVisibility(View.GONE);
                        }

                        // Scroll to the countdown card so user sees it immediately (if visible)
                         try {
                             NestedScrollView nsv = findViewById(R.id.nestedScrollView);
                             if (nsv != null && cardCountdown != null && cardCountdown.getVisibility() == View.VISIBLE) {
                                 nsv.post(() -> nsv.smoothScrollTo(0, cardCountdown.getTop()));
                             }
                         } catch (Exception ignored) {}
                     } catch (Exception e) {
                         Log.e(TAG, "Error parsing booking details", e);
                         Toast.makeText(PaymentActivity.this, "Không thể tải chi tiết đặt vé.", Toast.LENGTH_SHORT).show();
                         finish();
                     }
                 } else {
                     Toast.makeText(PaymentActivity.this, "Không thể tải chi tiết đặt vé.", Toast.LENGTH_SHORT).show();
                     finish();
                 }
             }

             @Override
             public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                 setLoadingState(false);
                 Toast.makeText(PaymentActivity.this, "Lỗi mạng khi tải chi tiết vé.", Toast.LENGTH_SHORT).show();
                 finish();
             }
         });
     }

    // Helper to check whether current selected payment method is QR-type
    private boolean selectedMethodIsQr() {
        int selected = getSelectedPaymentMethodId();
        return selected == R.id.rbQrPayment;
    }

    private void setupPromoHandlers() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        btnApplyPromo.setOnClickListener(v -> {
            // debounce fast repeated taps
            long now = System.currentTimeMillis();
            if (now - lastApplyClickMs < 800) return;
            lastApplyClickMs = now;

            String code = etPromoCode.getText() == null ? "" : etPromoCode.getText().toString().trim();
            if (appliedPromotionCode != null && appliedPromotionCode.equalsIgnoreCase(code)) {
                // Already applied -> clear
                clearAppliedPromotion();
                return;
            }
            if (code.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
                return;
            }
            // compute current subtotal
            double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip.getPrice() * seatLabels.size());

            // DEBUG: log click and subtotal
            Log.i(TAG, "PROMO apply clicked: code=" + code + ", subtotal=" + subtotal + ", appliedPromotionCode=" + appliedPromotionCode);

            // Show feedback to user
            Toast.makeText(this, "Đang kiểm tra mã " + code + "...", Toast.LENGTH_SHORT).show();

            btnApplyPromo.setEnabled(false);
            Map<String, Object> body = new HashMap<>();
            body.put("code", code);
            body.put("amount", subtotal);

            // DEBUG: log request body
            Log.d(TAG, "PROMO request body: " + body.toString());

            apiService.validatePromotion(body).enqueue(new Callback<vn.hcmute.busbooking.model.PromotionValidateResponse>() {
                @Override
                public void onResponse(Call<vn.hcmute.busbooking.model.PromotionValidateResponse> call, Response<vn.hcmute.busbooking.model.PromotionValidateResponse> response) {
                    btnApplyPromo.setEnabled(true);
                    // DEBUG: log response code and whether body present
                    try {
                        Log.i(TAG, "PROMO response: successful=" + response.isSuccessful() + ", code=" + response.code());
                        if (!response.isSuccessful()) {
                            String err = response.errorBody() != null ? response.errorBody().string() : "(no error body)";
                            Log.w(TAG, "PROMO response error body: " + err);
                        } else if (response.body() == null) {
                            Log.w(TAG, "PROMO response body is null");
                        } else {
                            vn.hcmute.busbooking.model.PromotionValidateResponse resDbg = response.body();
                            Log.i(TAG, "PROMO response body: valid=" + resDbg.isValid() + ", discount=" + resDbg.getDiscount() + ", final_amount=" + resDbg.getFinal_amount());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "PROMO response logging error", e);
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        vn.hcmute.busbooking.model.PromotionValidateResponse res = response.body();
                        Log.i(TAG, "✅ PROMO validation result: valid=" + res.isValid() + ", discount=" + res.getDiscount());

                        if (res.isValid()) {
                            try {
                                appliedPromotionCode = code;
                                appliedDiscount = res.getDiscount();
                                appliedPromotion = new HashMap<>();
                                if (res.getPromotion() != null) {
                                    vn.hcmute.busbooking.model.Promotion p = res.getPromotion();
                                    appliedPromotion.put("code", p.getCode());
                                    appliedPromotion.put("discount_type", p.getDiscount_type());
                                    appliedPromotion.put("discount_value", p.getDiscount_value());
                                    appliedPromotion.put("min_price", p.getMin_price());
                                    appliedPromotion.put("max_discount", p.getMax_discount());
                                    appliedPromotion.put("start_date", p.getStart_date());
                                    appliedPromotion.put("end_date", p.getEnd_date());
                                }

                                double newTotal = Math.max(0, subtotal - appliedDiscount);
                                Log.i(TAG, "💰 Applying discount: subtotal=" + subtotal + ", discount=" + appliedDiscount + ", newTotal=" + newTotal);

                                tvDiscountApplied.setText(getString(R.string.discount_applied, NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(appliedDiscount), NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal)));
                                tvDiscountApplied.setVisibility(View.VISIBLE);
                                tvPromoDetails.setVisibility(View.VISIBLE);
                                tvSubtotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(subtotal));
                                tvTotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal));
                                tvBottomTotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal));
                                btnApplyPromo.setText(getString(R.string.remove_promo));

                                // Show success toast
                                Toast.makeText(PaymentActivity.this,
                                    "✅ Áp dụng thành công! Giảm " + NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(appliedDiscount),
                                    Toast.LENGTH_LONG).show();

                                Log.i(TAG, "🎉 UI updated successfully!");
                            } catch (Exception ex) {
                                Log.e(TAG, "❌ Error applying promotion:", ex);
                                Toast.makeText(PaymentActivity.this, "Lỗi áp dụng mã: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            String reason = res.getReason();
                            if (reason == null) reason = getString(R.string.msg_invalid_promo);
                            // Map common reason keywords to user-friendly messages
                            String lower = reason.toLowerCase();
                            if (lower.contains("not exist") || lower.contains("không tồn tại")) {
                                reason = getString(R.string.msg_promo_not_found);
                            } else if (lower.contains("inactive") || lower.contains("không còn hiệu lực")) {
                                reason = getString(R.string.msg_promo_inactive);
                            } else if (lower.contains("expired") || lower.contains("hết hạn") || lower.contains("đã hết hạn")) {
                                reason = getString(R.string.msg_promo_expired);
                            } else if (lower.contains("not started") || lower.contains("chưa có hiệu lực")) {
                                reason = getString(R.string.msg_promo_not_started);
                            } else if (lower.contains(">=") || lower.contains("đơn hàng") || lower.contains("đơn hàng phải")) {
                                reason = getString(R.string.msg_promo_min_price, NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(res.getPromotion() != null ? res.getPromotion().getMin_price() : 0));
                            }
                            Toast.makeText(PaymentActivity.this, reason, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(PaymentActivity.this, getString(R.string.msg_invalid_promo), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<vn.hcmute.busbooking.model.PromotionValidateResponse> call, Throwable t) {
                    btnApplyPromo.setEnabled(true);
                    Log.e(TAG, "PROMO request failed", t);
                    String errorMsg = "Lỗi kết nối: ";
                    if (t instanceof java.net.SocketTimeoutException) {
                        errorMsg += "Timeout - server phản hồi quá lâu. Vui lòng thử lại.";
                    } else if (t instanceof java.net.UnknownHostException) {
                        errorMsg += "Không thể kết nối đến server.";
                    } else {
                        errorMsg += (t.getMessage() != null ? t.getMessage() : "Vui lòng thử lại");
                    }
                    Toast.makeText(PaymentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        });

        tvPromoDetails.setOnClickListener(v -> {
            if (appliedPromotion == null) return;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            StringBuilder sb = new StringBuilder();
            try {
                // Mã giảm giá
                if (appliedPromotion.get("code") != null) {
                    sb.append("Mã: ").append(appliedPromotion.get("code")).append("\n\n");
                }

                // Loại và giá trị giảm giá
                String discountType = appliedPromotion.get("discount_type") != null ?
                    String.valueOf(appliedPromotion.get("discount_type")).toLowerCase() : "";
                Object discountValueObj = appliedPromotion.get("discount_value");

                if (discountValueObj != null) {
                    try {
                        double discountValue = Double.parseDouble(String.valueOf(discountValueObj));
                        if ("percent".equals(discountType) || "percentage".equals(discountType)) {
                            sb.append("Giảm: ").append((int)discountValue).append("%").append("\n\n");
                        } else {
                            sb.append("Giảm: ").append(currencyFormat.format(discountValue)).append("\n\n");
                        }
                    } catch (NumberFormatException e) {
                        sb.append("Giảm: ").append(discountValueObj).append("\n\n");
                    }
                }

                // Điều kiện tối thiểu
                Object minPriceObj = appliedPromotion.get("min_price");
                if (minPriceObj != null) {
                    try {
                        double minPrice = Double.parseDouble(String.valueOf(minPriceObj));
                        if (minPrice > 0) {
                            sb.append("Đơn tối thiểu: ").append(currencyFormat.format(minPrice)).append("\n");
                        }
                    } catch (NumberFormatException ignored) {}
                }

                // Giảm tối đa
                Object maxDiscountObj = appliedPromotion.get("max_discount");
                if (maxDiscountObj != null) {
                    try {
                        double maxDiscount = Double.parseDouble(String.valueOf(maxDiscountObj));
                        if (maxDiscount > 0) {
                            sb.append("\nGiảm tối đa: ").append(currencyFormat.format(maxDiscount)).append("\n");
                        }
                    } catch (NumberFormatException ignored) {}
                }

                // Thời gian hiệu lực
                String startDateStr = appliedPromotion.get("start_date") != null ?
                    String.valueOf(appliedPromotion.get("start_date")) : null;
                String endDateStr = appliedPromotion.get("end_date") != null ?
                    String.valueOf(appliedPromotion.get("end_date")) : null;

                if (startDateStr != null || endDateStr != null) {
                    sb.append("\n⏰ Thời gian: ");

                    if (startDateStr != null) {
                        try {
                            Date startDate = inputDateFormat.parse(startDateStr);
                            sb.append(outputDateFormat.format(startDate));
                        } catch (ParseException e) {
                            // Try parsing without milliseconds
                            try {
                                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date startDate = altFormat.parse(startDateStr);
                                sb.append(outputDateFormat.format(startDate));
                            } catch (ParseException ex) {
                                sb.append(startDateStr);
                            }
                        }
                    }

                    if (startDateStr != null && endDateStr != null) {
                        sb.append(" - ");
                    }

                    if (endDateStr != null) {
                        try {
                            Date endDate = inputDateFormat.parse(endDateStr);
                            sb.append(outputDateFormat.format(endDate));
                        } catch (ParseException e) {
                            // Try parsing without milliseconds
                            try {
                                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date endDate = altFormat.parse(endDateStr);
                                sb.append(outputDateFormat.format(endDate));
                            } catch (ParseException ex) {
                                sb.append(endDateStr);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error formatting promo details", e);
                sb.append("Không thể hiển thị chi tiết");
            }

            new AlertDialog.Builder(PaymentActivity.this)
                .setTitle(getString(R.string.view_promo_details))
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
        });
    }

    private void clearAppliedPromotion() {
        appliedPromotionCode = null;
        appliedDiscount = 0.0;
        appliedPromotion = null;
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        double subtotal = (bookingTotalAmount != null) ? bookingTotalAmount : (trip.getPrice() * seatLabels.size());
        tvDiscountApplied.setVisibility(View.GONE);
        tvPromoDetails.setVisibility(View.GONE);
        tvSubtotal.setText(nf.format(subtotal));
        tvTotal.setText(nf.format(subtotal));
        tvBottomTotal.setText(nf.format(subtotal));
        btnApplyPromo.setText(getString(R.string.apply_promo));
         etPromoCode.setText("");
    }

    /**
     * Change payment method from online (QR/Card) to offline (cash/counter)
     * This will:
     * 1. Stop the countdown timer
     * 2. Call backend API to update payment method
     * 3. Show success message and navigate to My Bookings
     */
    private void changePaymentMethodToOffline() {
        if (bookingIds == null || bookingIds.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã đặt vé", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Đổi phương thức thanh toán")
                .setMessage("Bạn muốn đổi sang thanh toán tại nhà xe?\n\n" +
                        "• Vé sẽ KHÔNG bị tự động hủy\n" +
                        "• Bạn cần thanh toán trước khi lên xe\n" +
                        "• Không thể đổi lại sang QR/Card sau này")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    setLoadingState(true);

                    // Stop countdown timer immediately
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }

                    // Call API for each booking to change payment method
                    changePaymentMethodForBookings(0, "cash");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Recursively change payment method for all bookings
     */
    private void changePaymentMethodForBookings(int index, String paymentMethod) {
        changePaymentMethodForBookings(index, paymentMethod, null);
    }

    /**
     * Recursively change payment method for all bookings with callback
     */
    private void changePaymentMethodForBookings(int index, String paymentMethod, Runnable onSuccess) {
        Log.d(TAG, "changePaymentMethodForBookings: index=" + index +
                   ", paymentMethod=" + paymentMethod +
                   ", bookingIds.size=" + (bookingIds != null ? bookingIds.size() : "null"));

        if (index >= bookingIds.size()) {
            // All bookings updated successfully
            setLoadingState(false);
            Log.d(TAG, "All bookings updated successfully");

            // Normalize payment method to check if offline
            boolean isOffline = paymentMethod != null &&
                (paymentMethod.equalsIgnoreCase("cash") ||
                 paymentMethod.equalsIgnoreCase("offline") ||
                 paymentMethod.equalsIgnoreCase("cod") ||
                 paymentMethod.equalsIgnoreCase("counter"));

            if (isOffline) {
                Log.d(TAG, "Changed to offline payment");
                // Update flags
                isOfflinePayment = true;
                currentPaymentMethod = paymentMethod;

                // Hide countdown and change button for offline payments
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
                if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                if (btnChangeToOffline != null) btnChangeToOffline.setVisibility(View.GONE);

                // Clear expiry from SharedPreferences
                if (!bookingIds.isEmpty()) {
                    saveExpiryForBooking(bookingIds.get(0), -1L);
                }

                // Show success dialog
                new AlertDialog.Builder(this)
                        .setTitle("Thành công")
                        .setMessage("Đã đổi sang thanh toán tại nhà xe.\n\n" +
                                "Vé của bạn sẽ không bị tự động hủy. Vui lòng thanh toán trước khi lên xe.")
                        .setPositiveButton("Xem vé của tôi", (dialog, which) -> {
                            Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Log.d(TAG, "Changed to online payment: " + paymentMethod);
                // Update flags for online payment
                isOfflinePayment = false;
                currentPaymentMethod = paymentMethod;

                // ✅ Create new expiry timestamp for online payment (10 minutes from now)
                if (!bookingIds.isEmpty()) {
                    long newExpiry = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes
                    saveExpiryForBooking(bookingIds.get(0), newExpiry);

                    // Show countdown card
                    if (cardCountdown != null) {
                        cardCountdown.setVisibility(View.VISIBLE);
                    }

                    // Start countdown timer
                    startCountdown(10 * 60 * 1000);

                    Log.d(TAG, "Changed to online payment - created new expiry: " + new java.util.Date(newExpiry));
                }

                // Online payment - run callback if provided
                if (onSuccess != null) {
                    Log.d(TAG, "Running onSuccess callback for online payment");
                    onSuccess.run();
                } else {
                    Log.w(TAG, "onSuccess callback is null!");
                }
            }
            return;
        }

        int bookingId = bookingIds.get(index);
        Log.d(TAG, "Changing payment method for booking #" + bookingId + " to " + paymentMethod);

        Map<String, String> body = new HashMap<>();
        body.put("payment_method", paymentMethod);

        apiService.changePaymentMethod(bookingId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Changed payment method for booking " + bookingId + " to " + paymentMethod);
                    // Continue with next booking
                    changePaymentMethodForBookings(index + 1, paymentMethod, onSuccess);
                } else {
                    // Handle error
                    setLoadingState(false);
                    Log.e(TAG, "❌ Failed to change payment method. Response code: " + response.code());
                    String errorMsg = "Không thể đổi phương thức thanh toán";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                            JSONObject errorJson = new JSONObject(errorBody);
                            errorMsg = errorJson.optString("message", errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                    Toast.makeText(PaymentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "❌ Network error when changing payment method", t);
                Toast.makeText(PaymentActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Confirm change to offline payment with dialog
     */
    private void changePaymentMethodToOfflineConfirm(String paymentMethod) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đổi phương thức")
                .setMessage("Bạn muốn đổi sang thanh toán tại nhà xe?\n\n" +
                        "• Vé sẽ KHÔNG bị tự động hủy\n" +
                        "• Bạn cần thanh toán trước khi lên xe")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    setLoadingState(true);
                    changePaymentMethodForBookings(0, paymentMethod);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}

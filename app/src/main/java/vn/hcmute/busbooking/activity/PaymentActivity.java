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
    private Button btnConfirmPayment;
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

            if (isPendingPayment) {
                selectPaymentMethod(R.id.rbQrPayment);
                cardCreditCard.setClickable(false);
                rbCreditCard.setEnabled(false);
                cardPayAtOffice.setClickable(false);
                rbPayAtOffice.setEnabled(false);
                cardCreditCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.backgroundLight));
                cardPayAtOffice.setCardBackgroundColor(ContextCompat.getColor(this, R.color.backgroundLight));
                // Show countdown UI
                if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                // Ensure expiry is set in preferences for this booking
                if (bookingIds != null && !bookingIds.isEmpty()) {
                    long primary = bookingIds.get(0);
                    long expiry = getExpiryForBooking(primary);
                    if (expiry == -1L) {
                        expiry = System.currentTimeMillis() + HOLD_DURATION_MS;
                        saveExpiryForBooking(primary, expiry);
                    }
                    long remaining = expiry - System.currentTimeMillis();
                    if (remaining <= 0) {
                        onHoldExpired();
                    } else {
                        startCountdown(remaining);
                    }
                }
            }
        }

        btnConfirmPayment.setOnClickListener(v -> {
            if (isPendingPayment) {
                processPayosPayment(bookingIds);
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
        cardCountdown = findViewById(R.id.cardCountdown);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
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

        btnConfirmPayment.setText(getString(R.string.btn_confirm_payment, formatter.format(totalToShow)));
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
            if (isPendingPayment) return;

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
        String orderId = String.valueOf(System.currentTimeMillis());
        PaymentRequest request = new PaymentRequest(orderId, (int) (trip.getPrice() * seatLabels.size()), ids, fullName, email, phoneNumber);

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
        // If this was a pending payment flow, first verify payment status on server, then resume or start timer
        if (isPendingPayment && bookingIds != null && !bookingIds.isEmpty()) {
            int primaryId = bookingIds.get(0);
            // Call backend to verify whether payment was completed while the user was outside the app
            apiService.verifyBookingPayment(primaryId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> body = response.body();
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
                    long expiry = getExpiryForBooking(primaryId);
                    long now = System.currentTimeMillis();
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
        // Cancel bookings on server for all bookingIds
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

                        // Determine payment method for this booking (if present)
                        String paymentMethodStr = null;
                        if (data.get("payment_method") instanceof String) paymentMethodStr = (String) data.get("payment_method");
                        String statusStr = null;
                        if (data.get("status") instanceof String) statusStr = (String) data.get("status");
                        boolean isQrType = false;
                        if (paymentMethodStr != null) {
                            String lower = paymentMethodStr.toLowerCase(Locale.getDefault());
                            if (lower.contains("qr") || lower.contains("payos") || lower.contains("wallet") || lower.contains("e-")) {
                                isQrType = true;
                            }
                        }

                        // Save expiry derived from created_at if not already saved
                        Object createdObj = data.get("created_at");
                        long createdMs = -1L;
                        if (createdObj instanceof String) {
                            try {
                                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                Date d = iso.parse((String) createdObj);
                                if (d != null) createdMs = d.getTime();
                            } catch (Exception ignored) {}
                        }
                        long primary = bookingId;
                        long existingExpiry = getExpiryForBooking(primary);
                        long expiryToUse = existingExpiry;
                        if (existingExpiry == -1L) {
                            if (createdMs > 0) {
                                expiryToUse = createdMs + HOLD_DURATION_MS;
                            } else {
                                expiryToUse = System.currentTimeMillis() + HOLD_DURATION_MS;
                            }
                            saveExpiryForBooking(primary, expiryToUse);
                        }

                        // Continue initialization
                        populateBookingSummary();
                        setupPaymentMethodSelection();

                        // Start UI according to payment method and booking status. Only QR/e-wallet holds get the 10-min countdown.
                        if ("pending".equalsIgnoreCase(statusStr) && isQrType) {
                            // This booking is a QR/e-wallet pending hold -> show countdown and lock other payment options
                            isPendingPayment = true;
                            selectPaymentMethod(R.id.rbQrPayment);
                            cardCreditCard.setClickable(false);
                            rbCreditCard.setEnabled(false);
                            cardPayAtOffice.setClickable(false);
                            rbPayAtOffice.setEnabled(false);
                            if (cardCountdown != null) cardCountdown.setVisibility(View.VISIBLE);
                            // Ensure expiry exists and then start timer
                            long primaryId = primary;
                            long expiry = getExpiryForBooking(primaryId);
                            if (expiry == -1L) {
                                if (createdMs > 0) expiry = createdMs + HOLD_DURATION_MS; else expiry = System.currentTimeMillis() + HOLD_DURATION_MS;
                                saveExpiryForBooking(primaryId, expiry);
                            }
                            long now = System.currentTimeMillis();
                            if (expiry <= now) onHoldExpired(); else startCountdown(expiry - now);
                        } else {
                            // Not a QR hold - show normal UI (Pay button for pending cash, or QR optional)
                            isPendingPayment = false;
                            if (cardCountdown != null) cardCountdown.setVisibility(View.GONE);
                        }
                        // Scroll to the countdown card so user sees it immediately (if visible)
                         try {
                             NestedScrollView nsv = findViewById(R.id.nestedScrollView);
                             if (nsv != null) {
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
                        if (res.isValid()) {
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
                            tvDiscountApplied.setText(getString(R.string.discount_applied, NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(appliedDiscount), NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal)));
                            tvDiscountApplied.setVisibility(View.VISIBLE);
                            tvPromoDetails.setVisibility(View.VISIBLE);
                            tvSubtotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(subtotal));
                            tvTotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal));
                            btnConfirmPayment.setText(getString(R.string.btn_confirm_payment, NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(newTotal)));
                            btnApplyPromo.setText(getString(R.string.remove_promo));
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
                    Toast.makeText(PaymentActivity.this, getString(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvPromoDetails.setOnClickListener(v -> {
            if (appliedPromotion == null) return;
            StringBuilder sb = new StringBuilder();
            try {
                if (appliedPromotion.get("code") != null) sb.append("Mã: ").append(appliedPromotion.get("code")).append("\n");
                if (appliedPromotion.get("discount_type") != null) sb.append("Loại: ").append(appliedPromotion.get("discount_type")).append("\n");
                if (appliedPromotion.get("discount_value") != null) sb.append("Giá trị: ").append(appliedPromotion.get("discount_value")).append("\n");
                if (appliedPromotion.get("min_price") != null) sb.append("Điều kiện tối thiểu: ").append(appliedPromotion.get("min_price")).append("\n");
                if (appliedPromotion.get("max_discount") != null) sb.append("Giảm tối đa: ").append(appliedPromotion.get("max_discount")).append("\n");
                if (appliedPromotion.get("start_date") != null) sb.append("Bắt đầu: ").append(appliedPromotion.get("start_date")).append("\n");
                if (appliedPromotion.get("end_date") != null) sb.append("Kết thúc: ").append(appliedPromotion.get("end_date")).append("\n");
            } catch (Exception ignored) {}
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
        btnApplyPromo.setText(getString(R.string.apply_promo));
         etPromoCode.setText("");
    }
}

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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private TextView tvBusOperator, tvBusType, tvPickup, tvDropoff, tvDate, tvSeat, tvDepartureTime, tvArrivalTime;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;

    private MaterialCardView cardCreditCard, cardQrPayment, cardPayAtOffice;
    private RadioButton rbCreditCard, rbQrPayment, rbPayAtOffice;
    private LinearLayout creditCardForm;

    private Trip trip;
    private ArrayList<String> seatLabels;
    private ApiService apiService;
    private SessionManager sessionManager;

    private boolean isPendingPayment;
    private ArrayList<Integer> bookingIds;
    private String fullName, phoneNumber, email;

    private int pickupStopId;
    private int dropoffStopId;
    private String pickupStopName;
    private String dropoffStopName;

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
            Toast.makeText(this, "Lỗi: Dữ liệu đặt vé không hợp lệ.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        progressBar = new ProgressBar(this);
        cardCreditCard = findViewById(R.id.cardCreditCard);
        cardQrPayment = findViewById(R.id.cardQrPayment);
        cardPayAtOffice = findViewById(R.id.cardPayAtOffice);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        rbQrPayment = findViewById(R.id.rbQrPayment);
        rbPayAtOffice = findViewById(R.id.rbPayAtOffice);
        creditCardForm = findViewById(R.id.creditCardForm);
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

        if (trip == null || seatLabels == null || seatLabels.isEmpty() || pickupStopId == -1 || dropoffStopId == -1 || pickupStopName == null || dropoffStopName == null) {
            Log.e(TAG, "Missing critical booking data from Intent.");
            return false;
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
        btnConfirmPayment.setText("Thanh toán " + formatter.format(trip.getPrice() * seatLabels.size()));
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
            Toast.makeText(this, "Vui lòng đăng nhập để đặt vé", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getSelectedPaymentMethodId() == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("trip_id", trip.getId());
        body.put("seat_labels", seatLabels);
        body.put("pickup_stop_id", pickupStopId);
        body.put("dropoff_stop_id", dropoffStopId);

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
                            processPayment(newBookingIds);
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
                .setTitle("Ghế đã được đặt")
                .setMessage("Rất tiếc, một hoặc nhiều ghế bạn chọn đã được người khác đặt trong lúc bạn thanh toán. Vui lòng quay lại để chọn ghế khác.")
                .setPositiveButton("Chọn lại ghế", (dialog, which) -> {
                    // Go back to SeatSelectionActivity
                    Intent intent = new Intent(PaymentActivity.this, SeatSelectionActivity.class);
                    intent.putExtra("trip", trip);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy bỏ", (dialog, which) -> dialog.dismiss())
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
        Toast.makeText(PaymentActivity.this, "Đặt vé thành công!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(PaymentActivity.this, MyBookingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnConfirmPayment.setEnabled(!isLoading);
    }
}

package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class BookingDetailActivity extends AppCompatActivity {

    private static final String TAG = "BookingDetailActivity";

<<<<<<< Updated upstream
    private TextView tvOperatorName, tvStatus, tvOrigin, tvDepartureTime, 
                     tvDestination, tvArrivalDate, tvPassengerName, tvPhoneNumber, 
                     tvSeatNumber, tvLicensePlate, tvTotalAmount;
    private ImageView ivQrCode, ivOperatorLogo;
    private Button btnCancelTicket, btnPayTicket;
=======
    private TextView tvOperatorName, tvOrigin, tvDepartureTime,
                     tvDestination, tvPassengerName, tvPhoneNumber,
                     tvSeatNumber;
    private TextView tvPickupLocation, tvPickupAddress, tvDropoffLocation, tvDropoffAddress;
    private TextView tvCountdownTimer, tvBusType;
    private TextView tvPaymentMethodHeading, tvPaymentDetails;
    private TextView tvPaymentName, tvPaymentSubtext; 
    private View cardWaiting, cardPaymentMethod, qrCodeSection, actionButtonsContainer;
    private ImageView ivQrCode;
    private Button btnCancelTicket, btnPayTicket, btnChangePaymentMethod;
>>>>>>> Stashed changes
    private TextView tvQrHint;
    private Toolbar toolbar;

    private ApiService apiService;
    private int bookingId;
<<<<<<< Updated upstream
=======
    private CountDownTimer countDownTimer;
    private boolean isLoadingDetails = false;
>>>>>>> Stashed changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        Log.d(TAG, "onCreate() called");

        initializeViews();
        setupToolbar();

        apiService = ApiClient.getClient().create(ApiService.class);

        bookingId = getIntent().getIntExtra("booking_id", 0);

        if (bookingId == 0) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCancelTicket.setOnClickListener(v -> showCancelConfirmationDialog());
        btnPayTicket.setOnClickListener(v -> {
            Intent intent = new Intent(BookingDetailActivity.this, PaymentActivity.class);
            intent.putExtra("is_pending_payment", true);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(bookingId);
            intent.putIntegerArrayListExtra("booking_ids", ids);
<<<<<<< Updated upstream
            // also include booking-specific seat labels if present - the PaymentActivity expects seat_labels when creating payment flow
            // we can fetch seat labels from the loaded booking details via tvSeatNumber or from data map; but here we rely on PaymentActivity's pending flow which doesn't require seat_labels for pending payment
=======
            startActivity(intent);
        });
        
        btnChangePaymentMethod.setOnClickListener(v -> {
            Intent intent = new Intent(BookingDetailActivity.this, PaymentActivity.class);
            intent.putExtra("is_pending_payment", true);
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
            ids.add(bookingId);
            intent.putIntegerArrayListExtra("booking_ids", ids);
>>>>>>> Stashed changes
            startActivity(intent);
        });

        loadBookingDetails();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
<<<<<<< Updated upstream
=======

        cardWaiting = findViewById(R.id.cardWaiting);
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer);

>>>>>>> Stashed changes
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvDestination = findViewById(R.id.tvDestination);
<<<<<<< Updated upstream
        tvArrivalDate = findViewById(R.id.tvArrivalDate);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);
        tvLicensePlate = findViewById(R.id.tvLicensePlate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        ivQrCode = findViewById(R.id.ivQrCode);
        ivOperatorLogo = findViewById(R.id.ivOperatorLogo);
=======

        tvPickupLocation = findViewById(R.id.tvPickupLocation);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropoffLocation = findViewById(R.id.tvDropoffLocation);
        tvDropoffAddress = findViewById(R.id.tvDropoffAddress);

        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);

        tvPaymentMethodHeading = findViewById(R.id.tvPaymentMethodHeading);
        tvPaymentDetails = findViewById(R.id.tvPaymentDetails);
        cardPaymentMethod = findViewById(R.id.cardPaymentMethod);
        
        tvPaymentName = findViewById(R.id.tvPaymentName);
        tvPaymentSubtext = findViewById(R.id.tvPaymentSubtext);

        qrCodeSection = findViewById(R.id.qrCodeSection);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrHint = findViewById(R.id.tvQrHint);

        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
>>>>>>> Stashed changes
        btnCancelTicket = findViewById(R.id.btnCancelTicket);
        btnPayTicket = findViewById(R.id.btnPayTicket);
        tvQrHint = findViewById(R.id.tvQrHint);

<<<<<<< Updated upstream
        // default hide actions/QR until loaded
        ivQrCode.setVisibility(View.GONE);
        tvQrHint.setVisibility(View.GONE);
        btnCancelTicket.setVisibility(View.GONE);
        btnPayTicket.setVisibility(View.GONE);
=======
        if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
        if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
        if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);
        if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
        if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
>>>>>>> Stashed changes
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBookingDetails() {
        if (isLoadingDetails) return;
        isLoadingDetails = true;

        apiService.getBookingDetails(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoadingDetails = false;
                if (response.isSuccessful() && response.body() != null) {
                    displayBookingDetails(response.body());
                } else if (response.code() == 404) {
                    new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                            .setTitle("Vé không còn")
                            .setMessage("Vé chờ thanh toán của bạn đã hết hạn và đã bị xóa.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(BookingDetailActivity.this, "Không thể tải thông tin vé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoadingDetails = false;
                Toast.makeText(BookingDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookingDetails(Map<String, Object> data) {
<<<<<<< Updated upstream
        // Operator and Status
        tvOperatorName.setText((String) data.get("operator"));
        tvStatus.setText(getStatusText((String) data.get("status")));

        // Route and Time
        tvOrigin.setText((String) data.get("origin"));
        tvDestination.setText((String) data.get("destination"));
        tvDepartureTime.setText(formatTime((String) data.get("departure_time")));
        tvArrivalDate.setText(formatDate((String) data.get("arrival_time")));

        // Passenger and Seat Info
        tvPassengerName.setText((String) data.get("passenger_name"));
        tvPhoneNumber.setText((String) data.get("passenger_phone"));
        // seat_labels may be an array (new schema) or legacy seat_label string
        try {
            Object seatLabelsObj = data.get("seat_labels");
            String seatText = "";
            if (seatLabelsObj instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) seatLabelsObj;
                seatText = android.text.TextUtils.join(", ", list);
            } else if (seatLabelsObj instanceof String) {
                seatText = (String) seatLabelsObj;
            } else if (data.get("seat_label") instanceof String) {
                seatText = (String) data.get("seat_label");
=======
        String operator = (String) data.get("operator");
        if (tvOperatorName != null && operator != null) tvOperatorName.setText(operator.toUpperCase());

        String busType = (String) data.get("bus_type");
        if (tvBusType != null && busType != null) tvBusType.setText(busType);

        String origin = (String) data.get("origin");
        String destination = (String) data.get("destination");
        if (tvOrigin != null && origin != null) tvOrigin.setText(origin);
        if (tvDestination != null && destination != null) tvDestination.setText(destination);

        String departureTime = formatTime((String) data.get("departure_time"));
        String departureDate = formatDate((String) data.get("departure_time"));
        if (tvDepartureTime != null) tvDepartureTime.setText(departureTime + " • " + departureDate);

        if (tvPickupLocation != null) tvPickupLocation.setText((String) data.get("pickup_location"));
        if (tvPickupAddress != null) tvPickupAddress.setText((String) data.get("pickup_address"));
        if (tvDropoffLocation != null) tvDropoffLocation.setText((String) data.get("dropoff_location"));
        if (tvDropoffAddress != null) tvDropoffAddress.setText((String) data.get("dropoff_address"));

        if (tvPassengerName != null) tvPassengerName.setText((String) data.get("passenger_name"));
        if (tvPhoneNumber != null) tvPhoneNumber.setText((String) data.get("passenger_phone"));
        
        if (tvSeatNumber != null) {
            try {
                Object seatLabelsObj = data.get("seat_labels");
                if (seatLabelsObj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) seatLabelsObj;
                    StringBuilder sb = new StringBuilder();
                    for (Object obj : list) {
                        if (obj != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(obj.toString());
                        }
                    }
                    tvSeatNumber.setText(sb.toString());
                } else if (seatLabelsObj != null) {
                    tvSeatNumber.setText(String.valueOf(seatLabelsObj));
                } else {
                    Object singleLabel = data.get("seat_label");
                    tvSeatNumber.setText(singleLabel != null ? String.valueOf(singleLabel) : "N/A");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing seat number: " + e.getMessage());
                tvSeatNumber.setText("N/A");
>>>>>>> Stashed changes
            }
            tvSeatNumber.setText(seatText);
        } catch (Exception e) {
            tvSeatNumber.setText( (String) data.get("seat_label") );
        }
        tvLicensePlate.setText("51F-123.45"); // Placeholder, needs to come from API

<<<<<<< Updated upstream
        // Total Amount
        Object totalAmountObj = data.get("total_amount");
        double totalAmount = 0.0;
        if (totalAmountObj instanceof String) {
            try {
                totalAmount = Double.parseDouble((String) totalAmountObj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse total_amount string: " + totalAmountObj);
            }
        } else if (totalAmountObj instanceof Number) {
            totalAmount = ((Number) totalAmountObj).doubleValue();
        } 
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalAmount.setText(formatter.format(totalAmount));

        // Determine UI by status
        String status = (String) data.get("status");
        if ("pending".equals(status)) {
            // Pending: show Pay and Cancel buttons; no QR
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnPayTicket.setVisibility(View.VISIBLE);
            btnCancelTicket.setVisibility(View.VISIBLE);
        } else if ("confirmed".equals(status) || "completed".equals(status)) {
            // Confirmed/completed: show QR and Cancel (if applicable)
            ivQrCode.setVisibility(View.VISIBLE);
            tvQrHint.setVisibility(View.VISIBLE);
            generateQRCode(String.valueOf(bookingId));
            btnCancelTicket.setVisibility(View.VISIBLE);
            btnPayTicket.setVisibility(View.GONE);
        } else {
            // cancelled or other statuses: hide actions and QR
            ivQrCode.setVisibility(View.GONE);
            tvQrHint.setVisibility(View.GONE);
            btnCancelTicket.setVisibility(View.GONE);
            btnPayTicket.setVisibility(View.GONE);
=======
        Object pricePaidObj = data.get("price_paid");
        Object totalAmountObj = data.get("total_amount");
        double amount = 0.0;
        if (pricePaidObj instanceof Number) amount = ((Number) pricePaidObj).doubleValue();
        else if (pricePaidObj instanceof String) try { amount = Double.parseDouble((String) pricePaidObj); } catch (Exception e){}
        
        if (amount == 0 && totalAmountObj instanceof Number) amount = ((Number) totalAmountObj).doubleValue();
        else if (amount == 0 && totalAmountObj instanceof String) try { amount = Double.parseDouble((String) totalAmountObj); } catch (Exception e){}

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String amountStr = formatter.format(amount);
        if (btnPayTicket != null) btnPayTicket.setText("Thanh toán • " + amountStr);
        
        if (tvPaymentDetails != null) {
             StringBuilder details = new StringBuilder();
             details.append("Tổng tiền: ").append(amountStr);
             tvPaymentDetails.setText(details.toString());
        }

        String pm = (String) data.get("payment_method");
        String status = (String) data.get("status");
        
        String displayPmName = "Thanh toán";
        String displayPmSubtext = "";

        if (pm != null) {
            String lower = pm.toLowerCase();
            if (lower.contains("qr")) {
                displayPmName = "Mã thanh toán";
                displayPmSubtext = "Quét mã để thanh toán";
            } else if (lower.contains("card") || lower.contains("stripe")) {
                displayPmName = "Thẻ tín dụng";
                displayPmSubtext = "Visa, MasterCard, JCB";
            } else if (lower.contains("offline") || lower.contains("cash")) {
                displayPmName = "Tại nhà xe";
                displayPmSubtext = "Thanh toán khi lên xe";
            }
        }

        if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setText("Phương thức thanh toán");
        if (tvPaymentName != null) tvPaymentName.setText(displayPmName);
        if (tvPaymentSubtext != null) tvPaymentSubtext.setText(displayPmSubtext);

        boolean isOnlinePayment = false;
        if (pm != null) {
            String lower = pm.toLowerCase();
            if (lower.contains("qr") || lower.contains("card")) {
                isOnlinePayment = true;
            }
        }

        if ("cancelled".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
            hideAllActionCards();
            if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
        } else if ("pending".equalsIgnoreCase(status)) {
            if (isOnlinePayment) {
                 handlePendingCountdown(data, true);
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.VISIBLE);
                 if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.VISIBLE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
                 if (cardWaiting != null) cardWaiting.setVisibility(View.VISIBLE);
            } else {
                 if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
                 if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.VISIBLE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
            }
            if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.VISIBLE);
            if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.VISIBLE);
            if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.VISIBLE);

        } else if ("confirmed".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
             long arrivalTime = -1;
             try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = isoFormat.parse((String) data.get("arrival_time"));
                if (d != null) arrivalTime = d.getTime();
             } catch (Exception ignored) {}

             boolean isActuallyCompleted = "completed".equalsIgnoreCase(status) || (arrivalTime != -1 && System.currentTimeMillis() > arrivalTime);

             hideAllActionCards();
             
             if (isActuallyCompleted) {
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
             } else {
                 if (qrCodeSection != null) qrCodeSection.setVisibility(View.VISIBLE);
                 generateQRCode(String.valueOf(bookingId));
                 
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
                 if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.GONE);
             }
             
             if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.VISIBLE);
             if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.VISIBLE);
             if (btnChangePaymentMethod != null) btnChangePaymentMethod.setVisibility(View.GONE);
        }
    }
    
    private void hideAllActionCards() {
        if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
        if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
    }

    private void handlePendingCountdown(Map<String, Object> data, boolean isOnlinePayment) {
        if (countDownTimer != null) countDownTimer.cancel();
        
        String createdAtStr = (String) data.get("created_at");
        long createdMillis = -1;
        if (createdAtStr != null) {
             try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(createdAtStr);
                if (d != null) createdMillis = d.getTime();
            } catch (ParseException e) {
                try {
                     SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                     sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                     Date d2 = sdf2.parse(createdAtStr);
                     if (d2 != null) createdMillis = d2.getTime();
                } catch (ParseException ignored) {}
            }
        }

        if (createdMillis > 0) {
            long now = System.currentTimeMillis();
            long ttl = 10 * 60 * 1000L; 
            long millisInFuture = (createdMillis + ttl) - now;

            if (millisInFuture > 0) {
                if (tvCountdownTimer != null) {
                    countDownTimer = new CountDownTimer(millisInFuture, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            long seconds = millisUntilFinished / 1000;
                            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
                            tvCountdownTimer.setText(timeStr);
                        }
                        @Override
                        public void onFinish() {
                            Toast.makeText(BookingDetailActivity.this, "Vé đã hết hạn thanh toán", Toast.LENGTH_LONG).show();
                            loadBookingDetails();
                        }
                    }.start();
                }
            } else {
                 if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
            }
>>>>>>> Stashed changes
        }
    }

    private void generateQRCode(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Could not generate QR code", e);
        }
    }
    
    private String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        } catch (ParseException e) { return ""; }
    }

    private String formatDate(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));
            return dateFormat.format(date);
        } catch (ParseException e) { return ""; }
    }

<<<<<<< Updated upstream
    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "confirmed":
                return "Đã xác nhận";
            case "cancelled":
                return "Đã hủy";
            case "pending":
                return "Chờ thanh toán";
            case "completed":
                return "Đã hoàn thành";
            case "expired":
                return "Hết hạn";
            default:
                return status;
        }
    }

    private void showCancelConfirmationDialog() {
        // Simple confirmation - in real app show AlertDialog
        Toast.makeText(this, "Hủy vé clicked", Toast.LENGTH_SHORT).show();
=======
    private void showCancelConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy vé")
                .setMessage("Bạn có chắc chắn muốn hủy vé này không?")
                .setPositiveButton("Hủy vé", (dialog, which) -> cancelBooking())
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelBooking() {
        if (apiService == null) return;
        Toast.makeText(this, "Đang hủy vé...", Toast.LENGTH_SHORT).show();
        apiService.cancelBooking(bookingId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BookingDetailActivity.this, "Đã hủy vé", Toast.LENGTH_SHORT).show();
                    loadBookingDetails();
                } else {
                     try {
                        String errorBody = response.errorBody().string();
                        Toast.makeText(BookingDetailActivity.this, "Không thể hủy vé: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(BookingDetailActivity.this, "Không thể hủy vé", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BookingDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
>>>>>>> Stashed changes
    }
}

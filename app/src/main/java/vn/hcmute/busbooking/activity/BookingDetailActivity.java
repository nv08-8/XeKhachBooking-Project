package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
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

    private TextView tvOperatorName, tvOrigin, tvDepartureTime,
                     tvDestination, tvPassengerName, tvPhoneNumber,
                     tvSeatNumber;
    private TextView tvPickupLocation, tvPickupAddress, tvDropoffLocation, tvDropoffAddress;
    private TextView tvCountdownTimer, tvBusType, tvAppName;
    private TextView tvPaymentMethodHeading;
    private TextView tvPaymentName, tvPaymentSubtext;
    private TextView tvPaymentMethodName, tvPaymentMethodDesc;
    private TextView tvBasePrice, tvDiscount, tvTotalPrice;
    private TextView tvPromoCode, tvPromoCodeLabel;
    private ImageView ivPaymentMethodIcon;
    private View cardWaiting, cardPaymentMethod, cardPaymentMethodInfo, cardPriceBreakdown;
    private View qrCodeSection, actionButtonsContainer;
    private View discountRow, promoCodeRow;
    private ImageView ivQrCode;
    private Button btnCancelTicket, btnPayTicket;
    private TextView tvQrHint, btnChangePaymentInfo, btnChangePayment;
    private Toolbar toolbar;

    private ApiService apiService;
    private int bookingId;
    private CountDownTimer countDownTimer;
    private boolean isLoadingDetails = false;

    // Store booking data for payment processing
    private Map<String, Object> currentBookingData;

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
            Log.d(TAG, "btnPayTicket clicked - Processing payment directly for booking #" + bookingId);

            // Check payment method from currentBookingData
            if (currentBookingData != null) {
                String paymentMethod = (String) currentBookingData.get("payment_method");
                Log.d(TAG, "Current payment method: " + paymentMethod);

                if ("qr".equalsIgnoreCase(paymentMethod)) {
                    // QR payment - process directly via PayOS
                    processPayOSPayment();
                } else if ("card".equalsIgnoreCase(paymentMethod)) {
                    // Card payment
                    processCardPayment();
                } else {
                    // Offline or unknown - show dialog to choose payment method
                    showPaymentMethodSelectionDialog();
                }
            } else {
                // No booking data yet, show payment method selection
                showPaymentMethodSelectionDialog();
            }
        });

        // Set click listener for "Đổi" button in payment method card
        btnChangePayment.setOnClickListener(v -> {
            Log.d(TAG, "btnChangePayment clicked - Showing payment method selection dialog");
            showPaymentMethodSelectionDialog();
        });

        loadBookingDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload booking details when returning to this activity
        // This ensures data is refreshed after changing payment method or making payment
        Log.d(TAG, "onResume() - Force reloading booking details (bookingId=" + bookingId + ")");
        if (bookingId != 0) {
            // Force reload even if currently loading, to ensure fresh data
            loadBookingDetails();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);

        cardWaiting = findViewById(R.id.cardWaiting);
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer);

        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvBusType = findViewById(R.id.tvBusType);
        tvAppName = findViewById(R.id.tvAppName);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvDestination = findViewById(R.id.tvDestination);

        tvPickupLocation = findViewById(R.id.tvPickupLocation);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropoffLocation = findViewById(R.id.tvDropoffLocation);
        tvDropoffAddress = findViewById(R.id.tvDropoffAddress);

        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);

        // Payment method card for pending payments
        tvPaymentMethodHeading = findViewById(R.id.tvPaymentMethodHeading);
        cardPaymentMethod = findViewById(R.id.cardPaymentMethod);
        tvPaymentName = findViewById(R.id.tvPaymentName);
        tvPaymentSubtext = findViewById(R.id.tvPaymentSubtext);
        btnChangePayment = findViewById(R.id.btnChangePayment);

        // Payment method info card (for confirmed/completed bookings)
        cardPaymentMethodInfo = findViewById(R.id.cardPaymentMethodInfo);
        ivPaymentMethodIcon = findViewById(R.id.ivPaymentMethodIcon);
        tvPaymentMethodName = findViewById(R.id.tvPaymentMethodName);
        tvPaymentMethodDesc = findViewById(R.id.tvPaymentMethodDesc);
        btnChangePaymentInfo = findViewById(R.id.btnChangePaymentInfo);

        // Price breakdown card (separate card)
        cardPriceBreakdown = findViewById(R.id.cardPriceBreakdown);
        tvBasePrice = findViewById(R.id.tvBasePrice);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        discountRow = findViewById(R.id.discountRow);
        promoCodeRow = findViewById(R.id.promoCodeRow);
        tvPromoCode = findViewById(R.id.tvPromoCode);
        tvPromoCodeLabel = findViewById(R.id.tvPromoCodeLabel);

        qrCodeSection = findViewById(R.id.qrCodeSection);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrHint = findViewById(R.id.tvQrHint);

        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
        btnCancelTicket = findViewById(R.id.btnCancelTicket);
        btnPayTicket = findViewById(R.id.btnPayTicket);

        // Initially hide all action cards
        if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
        if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
        if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);
        if (cardPaymentMethodInfo != null) cardPaymentMethodInfo.setVisibility(View.GONE);
        if (cardPriceBreakdown != null) cardPriceBreakdown.setVisibility(View.GONE);
        if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
        if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
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
        // Store booking data for later use (e.g., payment processing)
        this.currentBookingData = data;

        String operator = (String) data.get("operator");
        if (tvOperatorName != null && operator != null) tvOperatorName.setText(operator.toUpperCase());

        String busType = (String) data.get("bus_type");
        if (tvBusType != null && busType != null) tvBusType.setText(busType);

        // Always set app name to GoUTE
        if (tvAppName != null) tvAppName.setText("GoUTE");

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
                // Build a list of seat label strings from multiple possible shapes
                java.util.List<String> labels = new java.util.ArrayList<>();

                if (seatLabelsObj instanceof java.util.List) {
                    for (Object obj : (java.util.List<?>) seatLabelsObj) {
                        if (obj == null) continue;
                        if (obj instanceof String) {
                            labels.add(((String) obj).trim());
                        } else if (obj instanceof Number) {
                            labels.add(String.valueOf(obj));
                        } else if (obj instanceof Map) {
                            Map<?, ?> m = (Map<?, ?>) obj;
                            Object lbl = null;
                            if (m.containsKey("label")) lbl = m.get("label");
                            if (lbl == null && m.containsKey("seat_label")) lbl = m.get("seat_label");
                            if (lbl == null && m.containsKey("seat_code")) lbl = m.get("seat_code");
                            if (lbl == null && m.containsKey("code")) lbl = m.get("code");
                            if (lbl == null && m.containsKey("seat")) lbl = m.get("seat");
                            if (lbl != null) labels.add(String.valueOf(lbl).trim());
                            else {
                                // if no known key found, try toString of the map
                                labels.add(m.toString());
                            }
                        } else {
                            labels.add(obj.toString());
                        }
                    }
                }
                // fallback: if still empty, try legacy 'seat' field
                if (labels.isEmpty()) {
                    Object seatObj = data.get("seat");
                    if (seatObj instanceof String) {
                        String s = ((String) seatObj).trim();
                        if (!s.isEmpty()) {
                            for (String p : s.split(",")) if (!p.trim().isEmpty()) labels.add(p.trim());
                        }
                    } else if (seatObj instanceof Number) {
                        labels.add(String.valueOf(seatObj));
                    }
                }

                // Remove empty & duplicate, preserve order
                java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
                for (String L : labels) {
                    if (L == null) continue;
                    String t = L.trim();
                    if (!t.isEmpty()) unique.add(t);
                }
                java.util.List<String> finalLabels = new java.util.ArrayList<>(unique);

                if (!finalLabels.isEmpty()) {
                    tvSeatNumber.setText(android.text.TextUtils.join(", ", finalLabels));
                } else {
                    tvSeatNumber.setText("N/A");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing seat number: " + e.getMessage());
                tvSeatNumber.setText("N/A");
            }
        }

        // Price calculation and breakdown
        Object pricePaidObj = data.get("price_paid");
        Object totalAmountObj = data.get("total_amount");
        Object basePriceObj = data.get("base_price");
        Object discountObj = data.get("discount_amount");
        Object promoCodeObj = data.get("promo_code");

        double totalAmount = 0.0;
        double basePrice = 0.0;
        double discountAmount = 0.0;
        String promoCode = null;

        // Get total amount
        if (pricePaidObj instanceof Number) totalAmount = ((Number) pricePaidObj).doubleValue();
        else if (pricePaidObj instanceof String) try { totalAmount = Double.parseDouble((String) pricePaidObj); } catch (Exception e){}

        if (totalAmount == 0 && totalAmountObj instanceof Number) totalAmount = ((Number) totalAmountObj).doubleValue();
        else if (totalAmount == 0 && totalAmountObj instanceof String) try { totalAmount = Double.parseDouble((String) totalAmountObj); } catch (Exception e){}

        // Get base price
        if (basePriceObj instanceof Number) basePrice = ((Number) basePriceObj).doubleValue();
        else if (basePriceObj instanceof String) try { basePrice = Double.parseDouble((String) basePriceObj); } catch (Exception e){}

        // Get discount
        if (discountObj instanceof Number) discountAmount = ((Number) discountObj).doubleValue();
        else if (discountObj instanceof String) try { discountAmount = Double.parseDouble((String) discountObj); } catch (Exception e){}

        // Get promo code
        if (promoCodeObj instanceof String) {
            promoCode = (String) promoCodeObj;
            if (promoCode != null && promoCode.trim().isEmpty()) promoCode = null;
        }

        // If base price is not provided, calculate it
        if (basePrice == 0 && totalAmount > 0) {
            basePrice = totalAmount + discountAmount;
        }

        // Debug logs
        Log.d(TAG, "Price breakdown - Base: " + basePrice + ", Discount: " + discountAmount + ", Total: " + totalAmount + ", PromoCode: " + promoCode);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String amountStr = formatter.format(totalAmount);

        // Update pay button
        if (btnPayTicket != null) btnPayTicket.setText("Thanh toán • " + amountStr);
        
        // Show price breakdown in separate card
        if (cardPriceBreakdown != null) {
            cardPriceBreakdown.setVisibility(View.VISIBLE);

            if (tvBasePrice != null) tvBasePrice.setText(formatter.format(basePrice));
            if (tvTotalPrice != null) tvTotalPrice.setText(formatter.format(totalAmount));

            // Show promo code row if promo code exists
            if (promoCode != null && promoCodeRow != null && tvPromoCode != null) {
                promoCodeRow.setVisibility(View.VISIBLE);
                tvPromoCode.setText(promoCode);
            } else if (promoCodeRow != null) {
                promoCodeRow.setVisibility(View.GONE);
            }

            // Show discount row only if there's a discount
            if (discountAmount > 0 && discountRow != null && tvDiscount != null) {
                Log.d(TAG, "Showing discount row with amount: " + discountAmount);
                discountRow.setVisibility(View.VISIBLE);
                tvDiscount.setText("-" + formatter.format(discountAmount));
            } else if (discountRow != null) {
                Log.d(TAG, "Hiding discount row - discountAmount: " + discountAmount + ", discountRow null: " + (discountRow == null) + ", tvDiscount null: " + (tvDiscount == null));
                discountRow.setVisibility(View.GONE);
            }
        }

        String pm = (String) data.get("payment_method");
        String status = (String) data.get("status");
        
        String displayPmName = "Thanh toán";
        String displayPmSubtext = "";
        String displayPmDesc = "Miễn phí thanh toán";
        int paymentIcon = R.drawable.ic_bus_payment;

        if (pm != null) {
            String lower = pm.toLowerCase();
            if (lower.contains("qr") || lower.contains("vnpay")) {
                displayPmName = "QR chuyển khoản";
                displayPmSubtext = "Quét mã để thanh toán";
                displayPmDesc = "Miễn phí thanh toán";
                paymentIcon = R.drawable.ic_qr_code;
            } else if (lower.contains("card") || lower.contains("stripe")) {
                displayPmName = "Thẻ tín dụng/ Thẻ ghi nợ";
                displayPmSubtext = "Visa, MasterCard, JCB";
                displayPmDesc = "Visa, MasterCard, JCB";
                paymentIcon = R.drawable.ic_credit_card_gray;
            } else if (lower.contains("offline") || lower.contains("cash")) {
                displayPmName = "Thanh toán tại nhà xe";
                displayPmSubtext = "Thanh toán khi lên xe";
                displayPmDesc = "Thanh toán khi lên xe";
                paymentIcon = R.drawable.ic_bus_payment;
            } else {
                // Use the raw payment method value
                displayPmName = pm;
                displayPmSubtext = "";
                displayPmDesc = pm;
            }
        }

        if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setText("Phương thức thanh toán");
        if (tvPaymentName != null) tvPaymentName.setText(displayPmName);
        if (tvPaymentSubtext != null) tvPaymentSubtext.setText(displayPmSubtext);

        // Update payment method info card (for confirmed/completed)
        if (tvPaymentMethodName != null) tvPaymentMethodName.setText(displayPmName);
        if (tvPaymentMethodDesc != null) tvPaymentMethodDesc.setText(displayPmDesc);
        if (ivPaymentMethodIcon != null) {
            try {
                ivPaymentMethodIcon.setImageResource(paymentIcon);
            } catch (Exception e) {
                // Use default icon if specific one not found
            }
        }

        boolean isOnlinePayment = false;
        if (pm != null) {
            String lower = pm.toLowerCase();
            if (lower.contains("qr") || lower.contains("card") || lower.contains("momo") || lower.contains("vnpay")) {
                isOnlinePayment = true;
            }
        }

        String normalizedStatus = status == null ? "" : status.toLowerCase();

        // Treat any cancellation/refund/expired-related status as non-actionable
        if (normalizedStatus.contains("cancel") || normalizedStatus.contains("refund") || normalizedStatus.contains("expired")) {
            hideAllActionCards();
            if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
            if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
            if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.GONE);
            // Hide both payment cards for cancelled tickets
            if (cardPaymentMethodInfo != null) cardPaymentMethodInfo.setVisibility(View.GONE);
            if (cardPriceBreakdown != null) cardPriceBreakdown.setVisibility(View.GONE);

        } else if ("pending".equalsIgnoreCase(status)) {
            // Pending payment - show action buttons and payment selection card
            if (isOnlinePayment) {
                 handlePendingCountdown(data, true);
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.VISIBLE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
                 if (cardWaiting != null) cardWaiting.setVisibility(View.VISIBLE);
            } else {
                 if (cardWaiting != null) cardWaiting.setVisibility(View.GONE);
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.VISIBLE);
                 if (btnPayTicket != null) btnPayTicket.setVisibility(View.GONE);
                 if (btnCancelTicket != null) btnCancelTicket.setVisibility(View.VISIBLE);
            }
            if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.VISIBLE);
            if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.VISIBLE);

            // Show "Đổi" button (listener already set in onCreate)
            if (btnChangePayment != null) {
                btnChangePayment.setVisibility(View.VISIBLE);
            }

            // Don't show payment method info card for pending
            if (cardPaymentMethodInfo != null) cardPaymentMethodInfo.setVisibility(View.GONE);

        } else if ("confirmed".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
             // Confirmed or completed booking
             long arrivalTime = -1;
             try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = isoFormat.parse((String) data.get("arrival_time"));
                if (d != null) arrivalTime = d.getTime();
             } catch (Exception ignored) {}

             boolean isActuallyCompleted = "completed".equalsIgnoreCase(status) || (arrivalTime != -1 && System.currentTimeMillis() > arrivalTime);

             hideAllActionCards();
             
             // Show payment method info card for paid tickets
             if (cardPaymentMethodInfo != null) cardPaymentMethodInfo.setVisibility(View.VISIBLE);

             if (isActuallyCompleted) {
                 // Completed - no actions available, no QR code
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
                 if (qrCodeSection != null) qrCodeSection.setVisibility(View.GONE);
             } else {
                 // Confirmed but not completed yet - show QR code
                 if (qrCodeSection != null) qrCodeSection.setVisibility(View.VISIBLE);
                 generateQRCode(String.valueOf(bookingId));
                 
                 // No action buttons for confirmed tickets (already paid)
                 if (actionButtonsContainer != null) actionButtonsContainer.setVisibility(View.GONE);
             }
             
             // Don't show payment method selection card for confirmed/completed
             if (cardPaymentMethod != null) cardPaymentMethod.setVisibility(View.GONE);
             if (tvPaymentMethodHeading != null) tvPaymentMethodHeading.setVisibility(View.GONE);
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
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            return dateFormat.format(date);
        } catch (ParseException e) { return ""; }
    }

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
    }

    private void showPaymentMethodSelectionDialog() {
        String[] paymentMethods = {
            "QR chuyển khoản",
            "Thẻ tín dụng/Thẻ ghi nợ",
            "Thanh toán tại nhà xe"
        };

        String[] paymentMethodValues = {"qr", "card", "offline"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn phương thức thanh toán")
                .setItems(paymentMethods, (dialog, which) -> {
                    String selectedMethod = paymentMethodValues[which];
                    String selectedMethodName = paymentMethods[which];
                    Log.d(TAG, "User selected payment method: " + selectedMethod);

                    // Show confirmation dialog
                    new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                            .setTitle("Xác nhận đổi phương thức")
                            .setMessage("Bạn có chắc muốn đổi sang phương thức \"" + selectedMethodName + "\" không?")
                            .setPositiveButton("Đồng ý", (d, w) -> {
                                changePaymentMethod(selectedMethod, selectedMethodName);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void changePaymentMethod(String newPaymentMethod, String methodName) {
        Log.d(TAG, "Changing payment method to: " + newPaymentMethod);

        // Show loading
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang đổi phương thức thanh toán...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Call API to change payment method
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("payment_method", newPaymentMethod);

        apiService.changePaymentMethod(bookingId, body).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Payment method changed successfully");
                    Toast.makeText(BookingDetailActivity.this,
                            "Đã đổi sang phương thức \"" + methodName + "\"",
                            Toast.LENGTH_SHORT).show();

                    // Reload booking details to update UI
                    loadBookingDetails();

                    // If user changed to online payment (QR/Card), ask to process payment immediately
                    if ("qr".equals(newPaymentMethod)) {
                        // QR Payment - Process payment directly via PayOS
                        new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                                .setTitle("Tiếp tục thanh toán?")
                                .setMessage("Phương thức thanh toán đã được cập nhật. Bạn có muốn thanh toán ngay không?")
                                .setPositiveButton("Thanh toán ngay", (d, w) -> {
                                    processPayOSPayment();
                                })
                                .setNegativeButton("Để sau", null)
                                .show();
                    } else if ("card".equals(newPaymentMethod)) {
                        // Card Payment - Process payment via Stripe/Card gateway
                        new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                                .setTitle("Tiếp tục thanh toán?")
                                .setMessage("Phương thức thanh toán đã được cập nhật. Bạn có muốn thanh toán ngay không?")
                                .setPositiveButton("Thanh toán ngay", (d, w) -> {
                                    processCardPayment();
                                })
                                .setNegativeButton("Để sau", null)
                                .show();
                    }
                    // For offline payment, just show success message (no payment needed now)
                } else {
                    String errorMsg = "Không thể đổi phương thức thanh toán";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                            // Try to parse error message
                            if (errorBody.contains("message")) {
                                org.json.JSONObject json = new org.json.JSONObject(errorBody);
                                errorMsg = json.optString("message", errorMsg);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }

                    Toast.makeText(BookingDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to change payment method", t);
                Toast.makeText(BookingDetailActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void processPayOSPayment() {
        Log.d(TAG, "🔵 processPayOSPayment() called for booking #" + bookingId);

        if (currentBookingData == null) {
            Log.e(TAG, "❌ currentBookingData is null, cannot process payment");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin vé. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang tạo link thanh toán...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get booking details
        double amount = 0;
        String name = "";
        String email = "";
        String phone = "";

        // Get amount from booking
        Object totalAmountObj = currentBookingData.get("total_amount");
        if (totalAmountObj instanceof Number) {
            amount = ((Number) totalAmountObj).doubleValue();
        } else if (totalAmountObj instanceof String) {
            try {
                amount = Double.parseDouble((String) totalAmountObj);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse total_amount", e);
            }
        }

        // Get passenger info
        Object passengerInfoObj = currentBookingData.get("passenger_info");
        if (passengerInfoObj instanceof Map) {
            Map<?, ?> passengerInfo = (Map<?, ?>) passengerInfoObj;
            Object nameObj = passengerInfo.get("name");
            if (nameObj != null) name = String.valueOf(nameObj);

            Object emailObj = passengerInfo.get("email");
            if (emailObj != null) email = String.valueOf(emailObj);

            Object phoneObj = passengerInfo.get("phone");
            if (phoneObj != null) phone = String.valueOf(phoneObj);
        }

        // Also try top-level fields
        if (name.isEmpty()) {
            Object nameObj = currentBookingData.get("passenger_name");
            if (nameObj != null) name = String.valueOf(nameObj);
        }
        if (phone.isEmpty()) {
            Object phoneObj = currentBookingData.get("passenger_phone");
            if (phoneObj != null) phone = String.valueOf(phoneObj);
        }

        Log.d(TAG, "Payment details - amount: " + amount + ", name: " + name + ", email: " + email + ", phone: " + phone);

        if (amount <= 0) {
            progressDialog.dismiss();
            Log.e(TAG, "❌ Invalid amount: " + amount);
            Toast.makeText(this, "Lỗi: Không tìm thấy số tiền thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create payment request
        java.util.ArrayList<Integer> bookingIds = new java.util.ArrayList<>();
        bookingIds.add(bookingId);

        // Use booking ID + timestamp suffix as orderCode (must be numeric and < 10 digits for PayOS)
        // Format: {bookingId}{last4digitsOfTimestamp}
        long timestamp = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(timestamp).substring(String.valueOf(timestamp).length() - 4);
        String orderId = bookingId + timestampSuffix; // e.g., "4285234" (7-8 digits)

        Log.d(TAG, "Creating PaymentRequest with orderId: " + orderId + ", amount: " + amount);

        vn.hcmute.busbooking.model.PaymentRequest paymentRequest = new vn.hcmute.busbooking.model.PaymentRequest(
            orderId,
            (int) amount, // Convert to int (VND doesn't have decimals)
            bookingIds,
            name,
            email,
            phone
        );

        Log.d(TAG, "Calling createPayosPayment API...");
        apiService.createPayosPayment(paymentRequest).enqueue(new retrofit2.Callback<vn.hcmute.busbooking.model.PaymentResponse>() {
            @Override
            public void onResponse(retrofit2.Call<vn.hcmute.busbooking.model.PaymentResponse> call,
                                 retrofit2.Response<vn.hcmute.busbooking.model.PaymentResponse> response) {
                progressDialog.dismiss();

                Log.d(TAG, "📨 PayOS API Response - Success: " + response.isSuccessful() + ", Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    vn.hcmute.busbooking.model.PaymentResponse paymentResponse = response.body();
                    String checkoutUrl = paymentResponse.getCheckoutUrl();

                    Log.d(TAG, "PaymentResponse received - checkoutUrl: " + checkoutUrl);

                    if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                        Log.d(TAG, "✅ Opening browser with URL: " + checkoutUrl);

                        try {
                            // Open payment URL in browser
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(checkoutUrl));
                            startActivity(browserIntent);

                            Log.d(TAG, "✅ Browser intent started successfully");

                            // Show info dialog
                            new androidx.appcompat.app.AlertDialog.Builder(BookingDetailActivity.this)
                                    .setTitle("Đang chuyển đến trang thanh toán")
                                    .setMessage("Vui lòng hoàn tất thanh toán trên trang web PayOS. Sau khi thanh toán xong, vé của bạn sẽ được xác nhận tự động.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Failed to open browser", e);
                            Toast.makeText(BookingDetailActivity.this,
                                    "Không thể mở trình duyệt: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "❌ checkoutUrl is null or empty");
                        Toast.makeText(BookingDetailActivity.this,
                                "Không thể tạo link thanh toán",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "❌ API call failed - Code: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot read error body", e);
                    }
                    Toast.makeText(BookingDetailActivity.this,
                            "Lỗi tạo thanh toán: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<vn.hcmute.busbooking.model.PaymentResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to create payment", t);
                Toast.makeText(BookingDetailActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processCardPayment() {
        Log.d(TAG, "Processing Card payment for booking #" + bookingId);

        // Show loading dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang xác nhận thanh toán...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Call confirm payment API with card method
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("payment_method", "card");

        apiService.confirmPayment(bookingId, body).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call,
                                 retrofit2.Response<java.util.Map<String, Object>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    Toast.makeText(BookingDetailActivity.this,
                            "Thanh toán thành công!",
                            Toast.LENGTH_SHORT).show();
                    loadBookingDetails();
                } else {
                    Toast.makeText(BookingDetailActivity.this,
                            "Xác nhận thanh toán thất bại",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to confirm payment", t);
                Toast.makeText(BookingDetailActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

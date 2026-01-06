package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.PromotionRequest;
import vn.hcmute.busbooking.utils.SessionManager;

public class PromotionFormActivity extends AppCompatActivity {

    private EditText etCode, etDiscountValue, etMinPrice, etMaxDiscount, etStartDate, etEndDate;
    private Spinner spinnerDiscountType, spinnerStatus;
    private Button btnSavePromotion;

    private ApiService apiService;
    private SessionManager sessionManager;
    private int editingPromotionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_form);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        etCode = findViewById(R.id.etCode);
        etDiscountValue = findViewById(R.id.etDiscountValue);
        etMinPrice = findViewById(R.id.etMinPrice);
        etMaxDiscount = findViewById(R.id.etMaxDiscount);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        spinnerDiscountType = findViewById(R.id.spinnerDiscountType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSavePromotion = findViewById(R.id.btnSavePromotion);

        if (getIntent() != null && getIntent().hasExtra("promotion_id")) {
            editingPromotionId = getIntent().getIntExtra("promotion_id", -1);
            if (editingPromotionId != -1) {
                setTitle("Sửa Khuyến mãi");
                loadPromotionDetails(editingPromotionId);
            } else {
                setTitle("Thêm Khuyến mãi");
            }
        } else {
            setTitle("Thêm Khuyến mãi");
        }

        btnSavePromotion.setOnClickListener(v -> savePromotion());
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "";
        }
        // Remove timezone info (e.g., "2025-12-31T23:59:59+00:00" -> "2025-12-31 23:59:59")
        // Also handles "2025-12-31T23:59:59.000Z" format
        try {
            if (dateTime.contains("T")) {
                // Replace T with space
                String cleaned = dateTime.replace("T", " ");
                // Remove timezone info (+HH:mm or Z or .millseconds)
                cleaned = cleaned.replaceAll("[+Z].*$", "");
                cleaned = cleaned.replaceAll("\\.\\d+.*$", "");
                return cleaned;
            }
            return dateTime;
        } catch (Exception e) {
            return dateTime;
        }
    }

    private String formatDateTimeForApi(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "";
        }
        try {
            // If input is date only (YYYY-MM-DD), add default time 00:00:00
            if (!dateTime.contains(" ") && !dateTime.contains("T")) {
                return dateTime + " 00:00:00";
            }
            // If input has T (ISO format), convert to YYYY-MM-DD HH:mm:ss
            if (dateTime.contains("T")) {
                String formatted = dateTime.replace("T", " ");
                // Remove timezone info (.000Z or +00:00 etc)
                formatted = formatted.replaceAll("\\.\\d+.*$", "");
                formatted = formatted.replaceAll("[+Z].*$", "");
                return formatted;
            }
            // Already in YYYY-MM-DD HH:mm:ss format
            return dateTime;
        } catch (Exception e) {
            return dateTime;
        }
    }

    private void loadPromotionDetails(int promotionId) {
        apiService.getPromotionById(sessionManager.getUserId(), promotionId).enqueue(new Callback<Promotion>() {
            @Override
            public void onResponse(Call<Promotion> call, Response<Promotion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Promotion promotion = response.body();
                    etCode.setText(promotion.getCode());
                    // Format numbers without .0
                    etDiscountValue.setText(String.valueOf((long) promotion.getDiscount_value()));
                    etMinPrice.setText(String.valueOf((long) promotion.getMin_price()));
                    etMaxDiscount.setText(String.valueOf((long) promotion.getMax_discount()));
                    // Show datetime in clean format (YYYY-MM-DD HH:mm:ss)
                    etStartDate.setText(formatDateTime(promotion.getStart_date()));
                    etEndDate.setText(formatDateTime(promotion.getEnd_date()));

                    // Set spinner selections
                    if ("percent".equalsIgnoreCase(promotion.getDiscount_type())) {
                        spinnerDiscountType.setSelection(0);
                    } else {
                        spinnerDiscountType.setSelection(1);
                    }

                    if ("active".equalsIgnoreCase(promotion.getStatus())) {
                        spinnerStatus.setSelection(0);
                    } else {
                        spinnerStatus.setSelection(1);
                    }
                } else {
                    Toast.makeText(PromotionFormActivity.this, "Không thể tải thông tin khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Promotion> call, Throwable t) {
                Toast.makeText(PromotionFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePromotion() {
        Promotion promotion = new Promotion();
        promotion.setCode(etCode.getText().toString());
        promotion.setDiscount_type(spinnerDiscountType.getSelectedItem().toString());
        promotion.setDiscount_value(Double.parseDouble(etDiscountValue.getText().toString()));
        promotion.setMin_price(Double.parseDouble(etMinPrice.getText().toString()));
        promotion.setMax_discount(Double.parseDouble(etMaxDiscount.getText().toString()));
        // Format datetime for API: add time if only date is provided
        promotion.setStart_date(formatDateTimeForApi(etStartDate.getText().toString()));
        promotion.setEnd_date(formatDateTimeForApi(etEndDate.getText().toString()));
        promotion.setStatus(spinnerStatus.getSelectedItem().toString());

        if (editingPromotionId != -1) {
            // Update existing promotion
            apiService.updatePromotion(sessionManager.getUserId(), editingPromotionId, promotion).enqueue(new Callback<Promotion>() {
                @Override
                public void onResponse(Call<Promotion> call, Response<Promotion> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PromotionFormActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PromotionFormActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Promotion> call, Throwable t) {
                    Toast.makeText(PromotionFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create new promotion using PromotionRequest to exclude id field
            PromotionRequest request = new PromotionRequest(promotion);
            apiService.createPromotion(sessionManager.getUserId(), request).enqueue(new Callback<Promotion>() {
                @Override
                public void onResponse(Call<Promotion> call, Response<Promotion> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PromotionFormActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PromotionFormActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Promotion> call, Throwable t) {
                    Toast.makeText(PromotionFormActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

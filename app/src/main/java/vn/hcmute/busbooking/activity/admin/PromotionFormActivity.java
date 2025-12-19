package vn.hcmute.busbooking.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.utils.SessionManager;

public class PromotionFormActivity extends AppCompatActivity {

    private EditText etCode, etDiscountValue, etMinPrice, etMaxDiscount, etStartDate, etEndDate;
    private Spinner spinnerDiscountType, spinnerStatus;
    private Button btnSavePromotion;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        btnSavePromotion.setOnClickListener(v -> savePromotion());
    }

    private void savePromotion() {
        Promotion promotion = new Promotion();
        promotion.setCode(etCode.getText().toString());
        promotion.setDiscountType(spinnerDiscountType.getSelectedItem().toString());
        promotion.setDiscountValue(Double.parseDouble(etDiscountValue.getText().toString()));
        promotion.setMinPrice(Double.parseDouble(etMinPrice.getText().toString()));
        promotion.setMaxDiscount(Double.parseDouble(etMaxDiscount.getText().toString()));
        promotion.setStartDate(etStartDate.getText().toString());
        promotion.setEndDate(etEndDate.getText().toString());
        promotion.setStatus(spinnerStatus.getSelectedItem().toString());

        apiService.createPromotion(sessionManager.getUserId(), promotion).enqueue(new Callback<Promotion>() {
            @Override
            public void onResponse(Call<Promotion> call, Response<Promotion> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PromotionFormActivity.this, "Lưu khuyến mãi thành công", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

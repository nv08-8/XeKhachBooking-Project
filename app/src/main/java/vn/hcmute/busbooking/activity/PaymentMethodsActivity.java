package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import vn.hcmute.busbooking.R;

public class PaymentMethodsActivity extends AppCompatActivity {

    private MaterialCardView cardPayOS, cardCreditCard, cardPayAtOffice;
    private RadioButton rbPayOS, rbCreditCard, rbPayAtOffice;
    private LinearLayout llCardForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        cardPayOS = findViewById(R.id.cardPayOS);
        cardCreditCard = findViewById(R.id.cardCreditCard);
        cardPayAtOffice = findViewById(R.id.cardPayAtOffice);

        rbPayOS = findViewById(R.id.rbPayOS);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        rbPayAtOffice = findViewById(R.id.rbPayAtOffice);

        llCardForm = findViewById(R.id.llCardForm);

        // Click listeners for cards
        cardPayOS.setOnClickListener(v -> selectMethod("payos"));
        cardCreditCard.setOnClickListener(v -> selectMethod("credit_card"));
        cardPayAtOffice.setOnClickListener(v -> selectMethod("office"));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String status = rbPayAtOffice.isChecked() ? "Chờ thanh toán" : "Đã chọn phương thức";
            Toast.makeText(this, "Đã lưu: " + status, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void selectMethod(String method) {
        // Reset all
        rbPayOS.setChecked(false);
        rbCreditCard.setChecked(false);
        rbPayAtOffice.setChecked(false);
        
        cardPayOS.setStrokeWidth(0);
        cardCreditCard.setStrokeWidth(0);
        cardPayAtOffice.setStrokeWidth(0);
        
        llCardForm.setVisibility(View.GONE);

        // Apply selection
        switch (method) {
            case "payos":
                rbPayOS.setChecked(true);
                cardPayOS.setStrokeWidth(4);
                break;
            case "credit_card":
                rbCreditCard.setChecked(true);
                cardCreditCard.setStrokeWidth(4);
                llCardForm.setVisibility(View.VISIBLE);
                break;
            case "office":
                rbPayAtOffice.setChecked(true);
                cardPayAtOffice.setStrokeWidth(4);
                break;
        }
    }
}

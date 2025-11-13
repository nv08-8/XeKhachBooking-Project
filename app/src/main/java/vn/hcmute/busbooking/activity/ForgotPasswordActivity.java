package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnResetPassword;
    private ImageView ivBack;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtEmail = findViewById(R.id.edtEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        ivBack = findViewById(R.id.ivBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnResetPassword.setOnClickListener(v -> sendRecoveryEmail());

        ivBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void sendRecoveryEmail() {
        String email = edtEmail.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ email hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Yêu cầu đã được gửi. Vui lòng kiểm tra email.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
                    intent.putExtra("user_email", email);
                    intent.putExtra("context", "forgot_password");
                    startActivity(intent);
                    finish(); // Close this activity to prevent it from reappearing
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Email không tồn tại trong hệ thống.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

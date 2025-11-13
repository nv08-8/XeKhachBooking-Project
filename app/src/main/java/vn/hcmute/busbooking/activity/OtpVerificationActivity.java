package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText edtOtp;
    private Button btnVerify;
    private TextView tvResendOtp;
    private ImageView ivBack;

    private String userEmail, userName, userPassword, verificationContext;

    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        edtOtp = findViewById(R.id.edtOtp);
        btnVerify = findViewById(R.id.btnVerify);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        ivBack = findViewById(R.id.ivBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        Intent intent = getIntent();
        userEmail = intent.getStringExtra("user_email"); // FIXED: Using the correct key "user_email"
        verificationContext = intent.getStringExtra("context");

        if (userEmail == null || verificationContext == null) {
            Toast.makeText(this, "Lỗi: Không nhận được email hoặc context.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if ("register".equals(verificationContext)) {
            userName = intent.getStringExtra("user_name");
            userPassword = intent.getStringExtra("user_password");
            if (userName == null || userPassword == null) {
                Toast.makeText(this, "Thiếu thông tin đăng ký!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        btnVerify.setOnClickListener(v -> verifyOtp());
        tvResendOtp.setOnClickListener(v -> resendOtp());
        ivBack.setOnClickListener(v -> finish());
    }

    private void verifyOtp() {
        String otp = edtOtp.getText().toString().trim();
        if (otp.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập mã OTP gồm 6 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", userEmail);
        body.put("otp", otp);

        apiService.verifyOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // LOG THÊM
                android.util.Log.d("OTP_DEBUG", "code=" + response.code() + ", body=" + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    Object successObj = response.body().get("success");
                    android.util.Log.d("OTP_DEBUG", "successObj=" + successObj + " (" +
                            (successObj == null ? "null" : successObj.getClass().getName()) + ")");

                    Boolean isSuccess = successObj instanceof Boolean
                            ? (Boolean) successObj
                            : null;

                    if (isSuccess != null && isSuccess) {
                        Toast.makeText(OtpVerificationActivity.this, "Xác thực OTP thành công!", Toast.LENGTH_SHORT).show();

                        if ("register".equals(verificationContext)) {
                            completeRegistration();
                        } else if ("forgot_password".equals(verificationContext)) {
                            Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
                            intent.putExtra("user_email", userEmail);
                            intent.putExtra("otp", otp);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(OtpVerificationActivity.this, "Mã OTP không hợp lệ hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OtpVerificationActivity.this, "Mã OTP không hợp lệ hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(OtpVerificationActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeRegistration() {
        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("name", userName);
        registerBody.put("email", userEmail);
        registerBody.put("password", userPassword);

        apiService.register(registerBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpVerificationActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(OtpVerificationActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(OtpVerificationActivity.this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(OtpVerificationActivity.this, "Không thể hoàn tất đăng ký: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendOtp() {
        Map<String, String> body = new HashMap<>();
        body.put("email", userEmail);

        apiService.forgotPassword(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpVerificationActivity.this, "Mã OTP mới đã được gửi.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OtpVerificationActivity.this, "Không thể gửi lại mã.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                 Toast.makeText(OtpVerificationActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

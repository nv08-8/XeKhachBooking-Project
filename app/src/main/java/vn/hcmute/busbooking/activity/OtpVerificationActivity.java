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

import org.json.JSONObject;

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

    // Đổi tên biến để nhất quán
    private String email, phone, name, password, context;

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
        // SỬA: Dùng đúng key đã gửi từ RegisterActivity
        email = intent.getStringExtra("email");
        phone = intent.getStringExtra("phone");
        name = intent.getStringExtra("name");
        password = intent.getStringExtra("password");
        context = intent.getStringExtra("context");

        if (email == null || context == null) {
            Toast.makeText(this, "Lỗi: Không nhận được email hoặc context.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if ("register".equals(context)) {
            if (name == null || password == null || phone == null) {
                Toast.makeText(this, "Lỗi: Thiếu thông tin đăng ký. Vui lòng thử lại từ đầu.", Toast.LENGTH_LONG).show();
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
        body.put("email", email);
        body.put("otp", otp);

        apiService.verifyOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object successObj = response.body().get("success");
                    if (Boolean.TRUE.equals(successObj)) {
                        Toast.makeText(OtpVerificationActivity.this, "Xác thực OTP thành công!", Toast.LENGTH_SHORT).show();

                    if ("register".equals(context)) {
                        completeRegistration();
                    } else if ("forgot_password".equals(context)) {
                        Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
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
        if (name == null || password == null || phone == null || email == null) {
            Toast.makeText(this, "Không thể hoàn tất đăng ký do thiếu thông tin.", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("name", name);
        registerBody.put("email", email);
        registerBody.put("phone", phone);
        registerBody.put("password", password);

        apiService.finishRegister(registerBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpVerificationActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(OtpVerificationActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    try {
                        if (response.errorBody() != null) {
                            JSONObject errorObj = new JSONObject(response.errorBody().string());
                            String errorMessage = errorObj.optString("message", "Đăng ký thất bại. Vui lòng thử lại.");
                            Toast.makeText(OtpVerificationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(OtpVerificationActivity.this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(OtpVerificationActivity.this, "Có lỗi xảy ra khi xử lý phản hồi.", Toast.LENGTH_SHORT).show();
                    }
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
        body.put("email", email);

        apiService.sendOtp(body).enqueue(new Callback<Map<String, Object>>() {
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

package vn.hcmute.busbooking.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.admin.AdminHomeActivity;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvForgot, tvRegister;
    private SwitchCompat chkRemember;
    private ApiService apiService;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);
        chkRemember = findViewById(R.id.chkRemember);

        String text = getString(R.string.signup_link);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvRegister.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvRegister.setText(Html.fromHtml(text));
        }

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        btnLogin.setOnClickListener(v -> doLogin());
        tvForgot.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        Log.d("LOGIN", "doLogin() called");
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // Show progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng nhập...\n(Lần đầu có thể chậm do server đang khởi động)");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Show a toast after 15 seconds to let user know we're still waiting
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                Toast.makeText(LoginActivity.this, "Server đang khởi động, vui lòng đợi...", Toast.LENGTH_LONG).show();
            }
        }, 15000);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        long startTime = System.currentTimeMillis();
        Log.d("LOGIN", "Request started at: " + startTime);

        apiService.login(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                long elapsed = System.currentTimeMillis() - startTime;
                Log.d("LOGIN", "Response received after " + elapsed + "ms, code=" + response.code());

                dismissProgress();
                btnLogin.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    String err = null;
                    try {
                        err = response.errorBody() != null ? response.errorBody().string() : "null";
                    } catch (Exception e) {
                        Log.e("LOGIN", "error reading errorBody", e);
                    }
                    Log.e("LOGIN_ERROR_BODY", String.valueOf(err));
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại (code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> res = response.body();
                Log.d("LOGIN_BODY", String.valueOf(res));

                Object msg = res.get("message");
                if (msg != null && msg.toString().contains("Đăng nhập thành công")) {

                    Map<String, Object> user = (Map<String, Object>) res.get("user");
                    // If server returned token at top-level, include it in the user map so SessionManager can persist it
                    if (res.get("token") instanceof String) {
                        user.put("token", (String) res.get("token"));
                    }

                    sessionManager.saveUser(user);
                    sessionManager.setRememberMe(chkRemember.isChecked());

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    // Determine if user is admin. The backend may return a 'role' field, or 'isAdmin'.
                    final boolean isAdmin = determineIfAdmin(user);

                    // Fetch full user info from API to get all fields (dob, gender, avatar, etc.)
                    // This ensures all data is persisted after logout/login
                    Integer userId = sessionManager.getUserId();
                    if (userId != null) {
                        apiService.getUserInfo(userId).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                Log.d("LOGIN", "getUserInfo response code: " + response.code());
                                if (response.isSuccessful() && response.body() != null) {
                                    Map<String, Object> fullUserData = response.body();
                                    // Extract user data from response
                                    Map<String, Object> userData = null;
                                    if (fullUserData.get("data") instanceof Map) {
                                        userData = (Map<String, Object>) fullUserData.get("data");
                                    } else if (fullUserData.get("user") instanceof Map) {
                                        userData = (Map<String, Object>) fullUserData.get("user");
                                    } else {
                                        userData = fullUserData;
                                    }

                                    if (userData != null) {
                                        Log.d("LOGIN", "Full userData keys: " + userData.keySet().toString());
                                        Log.d("LOGIN", "Full userData: " + userData.toString());

                                        // Update session with full user data including dob, gender, avatar
                                        String name = getStringValue(userData, "name", "hoten", "full_name");
                                        String email = getStringValue(userData, "email");
                                        String phone = getStringValue(userData, "sdt", "phone");
                                        String dob = getStringValue(userData, "dob", "date_of_birth", "dateOfBirth", "birth_date");
                                        String gender = getStringValue(userData, "gender", "gioi_tinh", "gioiTinh", "sex");
                                        String avatar = getStringValue(userData, "avatar", "avatar_url", "avatarUrl", "profile_picture");
                                        String role = getStringValue(userData, "role");

                                        Log.d("LOGIN", "Extracted from API: dob=" + dob + ", gender=" + gender + ", avatar=" + avatar);

                                        sessionManager.updateUserInfo(name, email, phone, dob, gender);
                                        if (!isEmpty(avatar)) {
                                            sessionManager.setUserAvatar(avatar);
                                        }
                                        if (!isEmpty(role)) {
                                            sessionManager.updateRole(role);
                                        }
                                        Log.d("LOGIN", "Full user info saved: dob=" + dob + ", gender=" + gender + ", avatar=" + avatar);
                                        Log.d("LOGIN", "Verify read back: dob=" + sessionManager.getUserDob() + ", gender=" + sessionManager.getUserGender());
                                    }
                                }

                                // Navigate to home screen after saving all data
                                navigateAfterLogin(isAdmin);
                            }

                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Log.e("LOGIN", "Failed to fetch full user info, proceeding anyway", t);
                                navigateAfterLogin(isAdmin);
                            }
                        });
                    } else {
                        navigateAfterLogin(isAdmin);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - startTime;
                Log.e("LOGIN_ERROR", "Request failed after " + elapsed + "ms: " + t.getMessage(), t);

                dismissProgress();
                btnLogin.setEnabled(true);

                String errorMsg = "Không thể kết nối máy chủ";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout") || t.getMessage().contains("timed out")) {
                        errorMsg = "Server không phản hồi trong 45s.\n\nĐề xuất:\n1. Mở trình duyệt và truy cập:\nhttps://refreshing-respect-production.up.railway.app/\n2. Đợi trang load xong (đánh thức server)\n3. Quay lại app và thử đăng nhập lại";
                    } else if (t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Không thể kết nối: Kiểm tra mạng internet";
                    } else {
                        errorMsg = "Lỗi kết nối: " + t.getMessage();
                    }
                }
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void navigateAfterLogin(boolean isAdmin) {
        Intent intent;
        if (isAdmin) {
            intent = new Intent(LoginActivity.this, AdminHomeActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean determineIfAdmin(Map<String, Object> user) {
        if (user == null) return false;

        Object roleObj = user.get("role");
        if (roleObj != null && "admin".equalsIgnoreCase(String.valueOf(roleObj))) {
            return true;
        }

        Object isAdminObj = user.get("isAdmin");
        if (isAdminObj != null) {
            try {
                return Boolean.parseBoolean(String.valueOf(isAdminObj));
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String str = value.toString().trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return null;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}

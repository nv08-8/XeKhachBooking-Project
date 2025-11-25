package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private EditText edtName, edtEmail, edtPhone;
    private Button btnSaveProfile;
    private ImageView ivBack;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Toolbar navigation back
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                Log.d(TAG, "Toolbar navigation clicked");
                finish();
            });
        }

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        ivBack = findViewById(R.id.ivBack);



        Log.d(TAG, "Views initialized, ivBack: " + (ivBack != null));

        // Load current user info
        loadUserInfo();

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish();
            });
            Log.d(TAG, "Back button click listener set");
        } else {
            Log.e(TAG, "ivBack is null!");
        }

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(this, vn.hcmute.busbooking.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_tickets) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_account) {
                    Intent intent = new Intent(this, UserAccountActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void loadUserInfo() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();

        if (name != null) edtName.setText(name);
        if (email != null) {
            edtEmail.setText(email);
            edtEmail.setEnabled(false);
        }

        Integer userId = sessionManager.getUserId();
        if (userId != null) {
            apiService.getUserInfo(userId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> userData = response.body(); // Đổi tên biến user -> userData để tránh nhầm lẫn

                        // --- XỬ LÝ SỐ ĐIỆN THOẠI ---
                        Object phoneObj = userData.get("sdt");
                        if (phoneObj != null) {
                            String phone;
                            if (phoneObj instanceof Number) {
                                phone = String.format("%.0f", ((Number) phoneObj).doubleValue());
                            } else {
                                phone = phoneObj.toString();
                            }
                            if (phone.length() == 9 && !phone.startsWith("0")) {
                                phone = "0" + phone;
                            }
                            edtPhone.setText(phone);
                        }

                        // --- XỬ LÝ ROLE (MỚI THÊM) ---
                        // Đảm bảo bạn đã ánh xạ edtRole trong onCreate hoặc ánh xạ tại đây
                        TextInputEditText edtRole = findViewById(R.id.edtRole);
                        Object roleObj = userData.get("role"); // Lấy key 'role' từ API trả về

                        if (roleObj != null) {
                            String roleStr = roleObj.toString().toUpperCase();
                            edtRole.setText(roleStr);
                        } else {
                            // Nếu không có role, mặc định là USER
                            edtRole.setText("USER");
                        }

                        Log.d(TAG, "User info loaded");
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "Failed to load user info", t);
                }
            });
        }
    }


    private void saveProfile() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSaveProfile.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("sdt", phone);

        Log.d(TAG, "Updating profile for userId: " + userId);

        apiService.updateUserInfo(userId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSaveProfile.setEnabled(true);

                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> res = response.body();
                    Log.d(TAG, "Response body: " + res.toString());

                    Object successObj = res.get("success");
                    boolean success = successObj != null && Boolean.TRUE.equals(successObj);

                    if (success) {
                        // Update session with new name
                        String email = sessionManager.getUserEmail();
                        sessionManager.saveSession(userId, name, email);

                        Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = res.get("message") != null ? res.get("message").toString() : "Cập nhật thất bại";
                        Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Không thể cập nhật thông tin", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Update failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Update error", t);
            }
        });
    }
}

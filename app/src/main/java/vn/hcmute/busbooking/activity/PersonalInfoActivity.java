package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class PersonalInfoActivity extends AppCompatActivity {

    public static final String EXTRA_UPDATED_NAME = "extra_updated_name";
    public static final String EXTRA_UPDATED_PHONE = "extra_updated_phone";
    public static final String EXTRA_UPDATED_DOB = "extra_updated_dob";
    public static final String EXTRA_UPDATED_GENDER = "extra_updated_gender";

    private static final String TAG = "PersonalInfoActivity";

    private SessionManager sessionManager;
    private ApiService apiService;
    private ImageView ivProfileImage;
    private TextView tvName, tvEmail, tvPhone, tvDob, tvGender, tvRole;
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDob = findViewById(R.id.tvDob);
        tvGender = findViewById(R.id.tvGender);
        tvRole = findViewById(R.id.tvRole);
        Button btnEditProfile = findViewById(R.id.btnEditProfile);

        // Toolbar back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Edit button
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String mergedName = coalesce(valueOrNull(data.getStringExtra(EXTRA_UPDATED_NAME)), sessionManager.getUserName());
                        String mergedPhone = coalesce(valueOrNull(data.getStringExtra(EXTRA_UPDATED_PHONE)), sessionManager.getUserPhone());
                        String mergedDob = coalesce(valueOrNull(data.getStringExtra(EXTRA_UPDATED_DOB)), sessionManager.getUserDob());
                        String mergedGender = coalesce(valueOrNull(data.getStringExtra(EXTRA_UPDATED_GENDER)), sessionManager.getUserGender());
                        String email = sessionManager.getUserEmail();

                        sessionManager.updateUserInfo(mergedName, email, mergedPhone, mergedDob, mergedGender);
                        applyUserData(mergedName, email, mergedPhone, mergedDob, mergedGender, sessionManager.getRole());
                    }
                    loadUserData();
                });

        btnEditProfile.setOnClickListener(v ->
                editProfileLauncher.launch(new Intent(this, EditProfileActivity.class)));

        // Load data on create
        loadUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning from edit screen
        loadUserData();
    }

    private void loadUserData() {
        applyUserData(
                sessionManager.getUserName(),
                sessionManager.getUserEmail(),
                sessionManager.getUserPhone(),
                sessionManager.getUserDob(),
                sessionManager.getUserGender(),
                sessionManager.getRole());

        Integer userId = sessionManager.getUserId();
        if (userId != null) {
            apiService.getUserInfo(userId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> userData = extractUserPayload(response.body());
                        if (userData == null) {
                            Log.e(TAG, "User payload missing");
                            return;
                        }

                        String apiName = coalesce(
                                valueOrNull(userData.get("name")),
                                coalesce(valueOrNull(userData.get("hoten")), valueOrNull(userData.get("full_name"))));
                        String apiEmail = valueOrNull(userData.get("email"));
                        String apiPhone = valueOrNull(userData.get("sdt"));
                        String apiDob = valueOrNull(userData.get("dob"));
                        String apiGender = valueOrNull(userData.get("gender"));
                        String apiRole = valueOrNull(userData.get("role"));

                        String mergedName = coalesce(apiName, sessionManager.getUserName());
                        String mergedEmail = coalesce(apiEmail, sessionManager.getUserEmail());
                        String mergedPhone = coalesce(apiPhone, sessionManager.getUserPhone());
                        String mergedDob = coalesce(apiDob, sessionManager.getUserDob());
                        String mergedGender = coalesce(apiGender, sessionManager.getUserGender());
                        String mergedRole = coalesce(apiRole, sessionManager.getRole());

                        sessionManager.updateUserInfo(mergedName, mergedEmail, mergedPhone, mergedDob, mergedGender);
                        if (!isEmpty(mergedRole)) {
                            sessionManager.updateRole(mergedRole);
                        }

                        applyUserData(mergedName, mergedEmail, mergedPhone, mergedDob, mergedGender, mergedRole);

                        Log.d(TAG, "User info synced from API");
                    } else {
                        Log.e(TAG, "API Response not successful or null body");
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "API sync failed", t);
                }
            });
        }
    }

    private void applyUserData(String name, String email, String phone, String dob, String gender, String role) {
        tvName.setText(!isEmpty(name) ? name : "Chưa cập nhật");
        tvEmail.setText(!isEmpty(email) ? email : "Chưa cập nhật");
        tvPhone.setText(!isEmpty(phone) ? phone : "Chưa cập nhật");
        tvDob.setText(!isEmpty(dob) ? dob : "Chưa cập nhật");
        tvGender.setText(!isEmpty(gender) ? gender : "Chưa cập nhật");
        tvRole.setText(!isEmpty(role) ? role.toUpperCase() : "USER");
    }

    private void loadProfileImage() {
        Integer userId = sessionManager.getUserId();
        if (userId != null) {
            apiService.getUserInfo(userId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> userData = extractUserPayload(response.body());
                        if (userData != null) {
                            String imageUrl = valueOrNull(userData.get("avatar"));
                            if (!isEmpty(imageUrl)) {
                                Glide.with(PersonalInfoActivity.this)
                                        .load(imageUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_default_profile)
                                        .error(R.drawable.ic_default_profile)
                                        .into(ivProfileImage);
                            } else {
                                ivProfileImage.setImageResource(R.drawable.ic_default_profile);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "Failed to load profile image", t);
                    ivProfileImage.setImageResource(R.drawable.ic_default_profile);
                }
            });
        }
    }

    private String coalesce(String preferred, String fallback) {
        return !isEmpty(preferred) ? preferred : fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractUserPayload(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        Object user = body.get("user");
        if (user instanceof Map) {
            return (Map<String, Object>) user;
        }
        return body;
    }

    private String valueOrNull(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}

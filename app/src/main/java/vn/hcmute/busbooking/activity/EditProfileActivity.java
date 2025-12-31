package vn.hcmute.busbooking.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.text.TextUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.database.UserProfileDatabase;
import vn.hcmute.busbooking.utils.SessionManager;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private TextInputEditText edtName, edtEmail, edtPhone, edtDob;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private Button btnSaveProfile;
    private SessionManager sessionManager;
    private ApiService apiService;
    private UserProfileDatabase userProfileDb;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        userProfileDb = new UserProfileDatabase(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtDob = findViewById(R.id.edtDob);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        prefillFromSession();
        loadUserInfo();

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        DatePickerDialog.OnDateSetListener date = (view, year, monthOfYear, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        edtDob.setOnClickListener(v -> new DatePickerDialog(EditProfileActivity.this,
                date, myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_account);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(this, vn.hcmute.busbooking.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_tickets) {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_account) {
                    // Already in the account section, just finish this activity to go back
                    finish();
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void loadUserInfo() {
        Integer userId = sessionManager.getUserId();
        if (userId != null) {
            apiService.getUserInfo(userId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> userData = extractUserPayload(response.body());
                        if (userData == null) {
                            Log.w(TAG, "User info payload is empty");
                            return;
                        }

                        String name = asString(coalesceName(userData));
                        if (!TextUtils.isEmpty(name)) {
                            edtName.setText(name);
                        }

                        String email = asString(userData.get("email"));
                        if (!TextUtils.isEmpty(email)) {
                            edtEmail.setText(email);
                            edtEmail.setEnabled(false);
                        }

                        String phone = normalizePhone(asString(userData.get("phone")));
                        if (!TextUtils.isEmpty(phone)) {
                            edtPhone.setText(phone);
                        }

                        String dob = asString(userData.get("dob"));
                        if (!TextUtils.isEmpty(dob)) {
                            edtDob.setText(dob);
                        }

                        String gender = asString(userData.get("gender"));
                        if (!TextUtils.isEmpty(gender)) {
                            selectGender(gender);
                        }


                        sessionManager.updateUserInfo(
                                name,
                                !TextUtils.isEmpty(email) ? email : sessionManager.getUserEmail(),
                                phone,
                                dob,
                                gender);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "Failed to load user info", t);
                }
            });
        }
    }

    private void prefillFromSession() {
        setTextIfNotEmpty(edtName, sessionManager.getUserName());
        String email = sessionManager.getUserEmail();
        if (!TextUtils.isEmpty(email)) {
            edtEmail.setText(email);
            edtEmail.setEnabled(false);
        }
        setTextIfNotEmpty(edtPhone, sessionManager.getUserPhone());
        setTextIfNotEmpty(edtDob, sessionManager.getUserDob());
        selectGender(sessionManager.getUserGender());
    }

    private void setTextIfNotEmpty(TextInputEditText view, String value) {
        if (!TextUtils.isEmpty(value)) {
            view.setText(value);
        }
    }

    private void selectGender(String gender) {
        if (TextUtils.isEmpty(gender)) {
            rgGender.clearCheck();
            return;
        }
        if (gender.equalsIgnoreCase(rbMale.getText().toString())) {
            rbMale.setChecked(true);
        } else if (gender.equalsIgnoreCase(rbFemale.getText().toString())) {
            rbFemale.setChecked(true);
        } else {
            rbOther.setChecked(true);
        }
    }

    private String normalizePhone(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;
        String normalized = phone.trim();
        if (normalized.length() == 9 && !normalized.startsWith("0")) {
            normalized = "0" + normalized;
        }
        return normalized;
    }

    private String asString(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private Object coalesceName(Map<String, Object> userData) {
        Object name = userData.get("name");
        if (name == null || name.toString().trim().isEmpty()) {
            name = userData.get("hoten");
        }
        if (name == null || name.toString().trim().isEmpty()) {
            name = userData.get("full_name");
        }
        return name;
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

    private void saveProfile() {
        final String name = edtName.getText().toString().trim();
        final String phone = edtPhone.getText().toString().trim();
        final String dob = edtDob.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedGenderId);
        final String gender;
        if (selectedRadioButton != null) {
            gender = selectedRadioButton.getText().toString();
        } else {
            gender = "";
        }

        if (name.isEmpty() || phone.isEmpty() || dob.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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
        body.put("phone", phone);
        body.put("dob", dob);
        body.put("gender", gender);

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
                        String email = sessionManager.getUserEmail();
                        // Save all fields to session
                        sessionManager.saveSession(userId, name, email);
                        sessionManager.updateUserInfo(name, email, phone, dob, gender);

                        // Save dob/gender to local database (independent from backend)
                        userProfileDb.saveUserProfile(userId, dob, gender, null);

                        Log.d(TAG, "Profile saved: name=" + name + ", phone=" + phone + ", dob=" + dob + ", gender=" + gender);
                        Log.d(TAG, "Verify after save - phone=" + sessionManager.getUserPhone() +
                              ", dob=" + sessionManager.getUserDob() + ", gender=" + sessionManager.getUserGender());

                        // Prepare full result for caller
                        Intent result = new Intent();
                        result.putExtra(PersonalInfoActivity.EXTRA_UPDATED_NAME, name);
                        result.putExtra(PersonalInfoActivity.EXTRA_UPDATED_PHONE, phone);
                        result.putExtra(PersonalInfoActivity.EXTRA_UPDATED_DOB, dob);
                        result.putExtra(PersonalInfoActivity.EXTRA_UPDATED_GENDER, gender);
                        setResult(RESULT_OK, result);

                        Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = res.get("message") != null ? res.get("message").toString() : "Cập nhật thất bại";
                        Log.d(TAG, "Success is false, message: " + message);
                        Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Không thể cập nhật thông tin";
                    if (!response.isSuccessful()) {
                        errorMsg += " (HTTP " + response.code() + ")";
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                            Log.d(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    if (response.body() == null) {
                        errorMsg += " - Response body is null";
                        Log.d(TAG, "Response body is null!");
                    }
                    Log.d(TAG, "Final error: " + errorMsg);
                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        edtDob.setText(sdf.format(myCalendar.getTime()));
    }
}
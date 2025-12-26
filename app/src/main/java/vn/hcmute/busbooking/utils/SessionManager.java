// app/src/main/java/vn/hcmute/busbooking/utils/SessionManager.java
package vn.hcmute.busbooking.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Map;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_ID = "user_id";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_DOB = "user_dob";
    private static final String KEY_GENDER = "user_gender";
    private static final String KEY_AVATAR = "user_avatar";
    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_ROLE = "user_role"; // Add key for role

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Backward compatible - old method with 3 parameters
    public void saveSession(int userId, String name, String email) {
        saveSession(userId, name, email, null);
    }

    // New method with phone support
    public void saveSession(int userId, String name, String email, String phone) {
        prefs.edit()
                .putInt(KEY_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .apply();
    }

    // Overload method for updating only name and email
    public void updateUserInfo(String name, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        putIfHasText(editor, KEY_NAME, name);
        putIfHasText(editor, KEY_EMAIL, email);
        editor.apply();
    }

    public void updateUserInfo(String name, String email, String phone, String dob, String gender) {
        SharedPreferences.Editor editor = prefs.edit();

        // Only update fields that have actual values
        // Don't overwrite existing data with nulls/empty strings
        if (!TextUtils.isEmpty(name)) {
            editor.putString(KEY_NAME, name.trim());
        }
        if (!TextUtils.isEmpty(email)) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        if (!TextUtils.isEmpty(phone)) {
            editor.putString(KEY_PHONE, phone.trim());
        }
        if (!TextUtils.isEmpty(dob)) {
            editor.putString(KEY_DOB, dob.trim());
        }
        if (!TextUtils.isEmpty(gender)) {
            editor.putString(KEY_GENDER, gender.trim());
        }
        editor.apply();

        android.util.Log.d("SessionManager", "updateUserInfo called: name=" + name + ", email=" + email +
              ", phone=" + phone + ", dob=" + dob + ", gender=" + gender);
        android.util.Log.d("SessionManager", "After updateUserInfo - stored dob=" + getUserDob() + ", gender=" + getUserGender());
    }

    private void putIfHasText(SharedPreferences.Editor editor, String key, String value) {
        String sanitized = sanitize(value);
        if (!TextUtils.isEmpty(sanitized)) {
            editor.putString(key, sanitized);
        }
    }

    private String sanitize(String value) {
        return value != null ? value.trim() : null;
    }

    public void updateRole(String role) {
        if (role != null && !role.isEmpty()) {
            prefs.edit().putString(KEY_ROLE, role.trim()).apply();
        }
    }

    /**
     * Save user from backend Map response
     * Used by LoginActivity to store user session after successful login
     */
    public void saveUser(Map<String, Object> user) {
        if (user == null) return;

        Object idObj = user.get("id");
        int userId = -1;
        if (idObj instanceof Number) {
            userId = ((Number) idObj).intValue();
        } else if (idObj instanceof String) {
            try {
                userId = Integer.parseInt((String) idObj);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        String name = (String) user.get("name");
        if (name == null || name.trim().isEmpty()) {
            if (user.get("full_name") instanceof String) {
                name = ((String) user.get("full_name")).trim();
            } else if (user.get("fullName") instanceof String) {
                name = ((String) user.get("fullName")).trim();
            } else if (user.get("fullname") instanceof String) {
                name = ((String) user.get("fullname")).trim();
            }
        }
        String email = (String) user.get("email");
        String phone = (String) user.get("phone");
        String dob = (String) user.get("dob");
        String gender = (String) user.get("gender");
        String avatar = (String) user.get("avatar");
        String role = (String) user.get("role"); // Get role from user map
        String token = null;
        if (user.get("token") instanceof String) token = (String) user.get("token");

        android.util.Log.d("SessionManager", "saveUser: dob=" + dob + ", gender=" + gender + ", avatar=" + avatar);

        if (userId != -1) {
            saveSession(userId, name, email, phone);
            SharedPreferences.Editor editor = prefs.edit();
            if (dob != null) editor.putString(KEY_DOB, dob.trim());
            if (gender != null) editor.putString(KEY_GENDER, gender.trim());
            if (avatar != null) editor.putString(KEY_AVATAR, avatar.trim());
            if (token != null) editor.putString(KEY_TOKEN, token);
            if (role != null) editor.putString(KEY_ROLE, role); // Save role
            editor.apply();

            android.util.Log.d("SessionManager", "Saved - getUserDob=" + getUserDob() + ", getUserGender=" + getUserGender() + ", getUserAvatar=" + getUserAvatar());
        }
    }

    public Integer getUserId() {
        // Prefer stored int; fallback to string if needed
        try {
            int id = prefs.getInt(KEY_ID, -1);
            return id == -1 ? null : id;
        } catch (ClassCastException e) {
            String idStr = prefs.getString(KEY_ID, null);
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    prefs.edit().putInt(KEY_ID, id).apply();
                    return id;
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getUserPhone() {
        return prefs.getString(KEY_PHONE, null);
    }

    public String getUserDob() {
        return prefs.getString(KEY_DOB, null);
    }

    public String getUserGender() {
        return prefs.getString(KEY_GENDER, null);
    }

    public String getUserAvatar() {
        return prefs.getString(KEY_AVATAR, null);
    }

    public void setUserAvatar(String avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            prefs.edit().putString(KEY_AVATAR, avatar.trim()).apply();
        }
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        // A user is considered logged in if their user ID is saved.
        return getUserId() != null;
    }

    // Method to check if the current user is an admin
    public boolean isAdmin() {
        String role = prefs.getString(KEY_ROLE, "user"); // Default to "user"
        return "admin".equalsIgnoreCase(role);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // Alias for backward compatibility
    public void logout() {
        clearSession();
    }
}

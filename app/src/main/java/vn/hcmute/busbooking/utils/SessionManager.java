// app/src/main/java/vn/hcmute/busbooking/utils/SessionManager.java
package vn.hcmute.busbooking.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_ID = "user_id";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHONE = "user_phone";

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
        String email = (String) user.get("email");
        String phone = (String) user.get("phone");

        if (userId != -1) {
            saveSession(userId, name, email, phone);
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

    public boolean isLoggedIn() {
        // A user is considered logged in if their user ID is saved.
        return getUserId() != null;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // Alias for backward compatibility
    public void logout() {
        clearSession();
    }
}

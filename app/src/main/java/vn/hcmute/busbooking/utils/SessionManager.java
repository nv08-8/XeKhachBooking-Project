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

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(int userId, String name, String email) {
        prefs.edit()
                .putInt(KEY_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
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
        if (idObj instanceof Double) {
            userId = ((Double) idObj).intValue();
        } else if (idObj instanceof Integer) {
            userId = (Integer) idObj;
        }

        String name = (String) user.get("name");
        String email = (String) user.get("email");

        if (userId != -1) {
            saveSession(userId, name, email);
        }
    }

    public Integer getUserId() {
        // Handle both String and Int for backward compatibility
        try {
            int id = prefs.getInt(KEY_ID, -1);
            return id == -1 ? null : id;
        } catch (ClassCastException e) {
            // If stored as String (old version), parse it
            String idStr = prefs.getString(KEY_ID, null);
            if (idStr != null) {
                try {
                    int id = Integer.parseInt(idStr);
                    // Migrate to Int storage
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

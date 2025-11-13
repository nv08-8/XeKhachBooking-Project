package vn.hcmute.busbooking.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // ─────────────────────────────────────────────
    // 🔹 Lưu token nếu backend có trả token
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    // ─────────────────────────────────────────────
    // 🔹 Lưu user từ Map BE trả về
    public void saveUser(Map<String, Object> user) {
        if (user == null) return;

        // user.get("id") trả về Double → cần convert về int/string
        Object idObj = user.get("id");
        String id = idObj != null ? String.valueOf(idObj) : "";

        editor.putString(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, String.valueOf(user.get("name")));
        editor.putString(KEY_USER_EMAIL, String.valueOf(user.get("email")));

        editor.apply();
    }

    // ─────────────────────────────────────────────
    // 🔹 Lấy dữ liệu user
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    // ─────────────────────────────────────────────
    // 🔹 Xoá session khi logout
    public void logout() {
        editor.clear();
        editor.apply();
    }
}

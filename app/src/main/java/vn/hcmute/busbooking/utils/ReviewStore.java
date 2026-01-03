package vn.hcmute.busbooking.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ReviewStore {
    private static final String PREF_NAME = "ReviewPrefs";
    private static final String KEY_REVIEW_PREFIX = "review_";
    private final SharedPreferences prefs;

    public ReviewStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLocalReview(int bookingId, int tripId, int rating, String comment) {
        prefs.edit().putBoolean(KEY_REVIEW_PREFIX + bookingId, true).apply();
    }

    public boolean hasLocalReview(int bookingId) {
        return prefs.getBoolean(KEY_REVIEW_PREFIX + bookingId, false);
    }
}

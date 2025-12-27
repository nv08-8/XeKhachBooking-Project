package vn.hcmute.busbooking.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserProfileDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user_profile.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USER_PROFILE = "user_profile";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_DOB = "dob";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_AVATAR = "avatar";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    public UserProfileDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USER_PROFILE + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_DOB + " TEXT,"
                + COLUMN_GENDER + " TEXT,"
                + COLUMN_AVATAR + " TEXT,"
                + COLUMN_UPDATED_AT + " LONG)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PROFILE);
        onCreate(db);
    }

    /**
     * Save user profile (dob, gender, avatar)
     */
    public void saveUserProfile(int userId, String dob, String gender, String avatar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_DOB, dob);
        values.put(COLUMN_GENDER, gender);
        values.put(COLUMN_AVATAR, avatar);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());

        // Insert or update
        db.insertWithOnConflict(TABLE_USER_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        android.util.Log.d("UserProfileDB", "Saved profile: userId=" + userId + ", dob=" + dob + ", gender=" + gender);
    }

    /**
     * Get user profile
     */
    public UserProfile getUserProfile(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER_PROFILE,
                new String[]{COLUMN_USER_ID, COLUMN_DOB, COLUMN_GENDER, COLUMN_AVATAR},
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        UserProfile profile = null;
        if (cursor != null && cursor.moveToFirst()) {
            profile = new UserProfile(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            );
            cursor.close();
        }
        db.close();

        android.util.Log.d("UserProfileDB", "Retrieved profile: userId=" + userId +
              ", dob=" + (profile != null ? profile.dob : "null") +
              ", gender=" + (profile != null ? profile.gender : "null"));

        return profile;
    }

    /**
     * Delete user profile
     */
    public void deleteUserProfile(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER_PROFILE, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});
        db.close();
    }

    /**
     * Simple data class for user profile
     */
    public static class UserProfile {
        public int userId;
        public String dob;
        public String gender;
        public String avatar;

        public UserProfile(int userId, String dob, String gender, String avatar) {
            this.userId = userId;
            this.dob = dob;
            this.gender = gender;
            this.avatar = avatar;
        }
    }
}


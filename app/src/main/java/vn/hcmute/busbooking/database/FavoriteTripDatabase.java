package vn.hcmute.busbooking.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.model.Trip;

public class FavoriteTripDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorite_trips.db";
    // Increased version to add trip_id column
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_FAVORITE_TRIPS = "favorite_trips";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TRIP_ID = "trip_id"; // The actual trip ID from server
    private static final String COLUMN_ROUTE_ID = "route_id";
    private static final String COLUMN_USER_ID = "user_id"; // New Column
    private static final String COLUMN_OPERATOR = "operator";
    private static final String COLUMN_BUS_TYPE = "bus_type";
    private static final String COLUMN_DEPARTURE_TIME = "departure_time";
    private static final String COLUMN_ARRIVAL_TIME = "arrival_time";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_SEATS_TOTAL = "seats_total";
    private static final String COLUMN_SEATS_AVAILABLE = "seats_available";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_ORIGIN = "origin";
    private static final String COLUMN_DESTINATION = "destination";
    private static final String COLUMN_DISTANCE_KM = "distance_km";
    private static final String COLUMN_DURATION_MIN = "duration_min";

    public FavoriteTripDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_FAVORITE_TRIPS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRIP_ID + " INTEGER,"
                + COLUMN_ROUTE_ID + " INTEGER,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_OPERATOR + " TEXT,"
                + COLUMN_BUS_TYPE + " TEXT,"
                + COLUMN_DEPARTURE_TIME + " TEXT,"
                + COLUMN_ARRIVAL_TIME + " TEXT,"
                + COLUMN_PRICE + " REAL,"
                + COLUMN_SEATS_TOTAL + " INTEGER,"
                + COLUMN_SEATS_AVAILABLE + " INTEGER,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_ORIGIN + " TEXT,"
                + COLUMN_DESTINATION + " TEXT,"
                + COLUMN_DISTANCE_KM + " INTEGER,"
                + COLUMN_DURATION_MIN + " INTEGER" + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE_TRIPS);
        onCreate(db);
    }

    public void addFavoriteTrip(Trip trip, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int tripId = trip.getId();
        Integer routeIdObj = trip.getRouteId();
        int routeId = (routeIdObj != null && routeIdObj != 0) ? routeIdObj : tripId;

        // Check if trip already exists in favorites for this user (check by trip_id)
        Cursor cursor = db.query(
            TABLE_FAVORITE_TRIPS,
            new String[]{COLUMN_ID},
            COLUMN_TRIP_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
            new String[]{String.valueOf(tripId), String.valueOf(userId)},
            null, null, null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        // Only insert if not already exists
        if (!exists) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TRIP_ID, tripId); // Store the actual trip ID
            values.put(COLUMN_ROUTE_ID, routeId);
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_OPERATOR, trip.getOperator());
            values.put(COLUMN_BUS_TYPE, trip.getBusType());
            values.put(COLUMN_DEPARTURE_TIME, trip.getDepartureTime());
            values.put(COLUMN_ARRIVAL_TIME, trip.getArrivalTime());
            values.put(COLUMN_PRICE, trip.getPrice());
            values.put(COLUMN_SEATS_TOTAL, trip.getSeatsTotal());
            values.put(COLUMN_SEATS_AVAILABLE, trip.getSeatsAvailable());
            values.put(COLUMN_STATUS, trip.getStatus());
            values.put(COLUMN_CREATED_AT, trip.getCreatedAt());
            values.put(COLUMN_ORIGIN, trip.getOrigin());
            values.put(COLUMN_DESTINATION, trip.getDestination());
            values.put(COLUMN_DISTANCE_KM, trip.getDistanceKm());
            values.put(COLUMN_DURATION_MIN, trip.getDurationMin());
            db.insert(TABLE_FAVORITE_TRIPS, null, values);
        }

        db.close();
    }

    public void removeFavoriteTrip(int tripId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITE_TRIPS, COLUMN_TRIP_ID + " = ? AND " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(tripId), String.valueOf(userId)});
        db.close();
    }

    public boolean isFavorite(int tripId, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITE_TRIPS, new String[]{COLUMN_ID},
                COLUMN_TRIP_ID + " = ? AND " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(tripId), String.valueOf(userId)}, null, null, null);
        boolean isFavorite = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isFavorite;
    }

    @SuppressLint("Range")
    public List<Trip> getAllFavoriteTrips(int userId) {
        List<Trip> favoriteTrips = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FAVORITE_TRIPS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Trip trip = new Trip();
                // Set ID from trip_id (the actual trip ID from server)
                trip.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_TRIP_ID)));
                trip.setRouteId(cursor.getInt(cursor.getColumnIndex(COLUMN_ROUTE_ID)));
                trip.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_OPERATOR)));
                trip.setBusType(cursor.getString(cursor.getColumnIndex(COLUMN_BUS_TYPE)));
                trip.setDepartureTime(cursor.getString(cursor.getColumnIndex(COLUMN_DEPARTURE_TIME)));
                trip.setArrivalTime(cursor.getString(cursor.getColumnIndex(COLUMN_ARRIVAL_TIME)));
                trip.setPrice(cursor.getDouble(cursor.getColumnIndex(COLUMN_PRICE)));
                trip.setSeatsTotal(cursor.getInt(cursor.getColumnIndex(COLUMN_SEATS_TOTAL)));
                trip.setSeatsAvailable(cursor.getInt(cursor.getColumnIndex(COLUMN_SEATS_AVAILABLE)));
                trip.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_STATUS)));
                trip.setCreatedAt(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT)));
                trip.setOrigin(cursor.getString(cursor.getColumnIndex(COLUMN_ORIGIN)));
                trip.setDestination(cursor.getString(cursor.getColumnIndex(COLUMN_DESTINATION)));
                trip.setDistanceKm(cursor.getInt(cursor.getColumnIndex(COLUMN_DISTANCE_KM)));
                trip.setDurationMin(cursor.getInt(cursor.getColumnIndex(COLUMN_DURATION_MIN)));
                favoriteTrips.add(trip);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return favoriteTrips;
    }

    /**
     * Remove duplicate favorite trips for a user
     * Keeps only the first occurrence of each trip_id
     */
    public void removeDuplicateFavorites(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete duplicates, keeping only the one with MIN(id) for each trip_id
        db.execSQL(
            "DELETE FROM " + TABLE_FAVORITE_TRIPS +
            " WHERE " + COLUMN_ID + " NOT IN (" +
            "   SELECT MIN(" + COLUMN_ID + ")" +
            "   FROM " + TABLE_FAVORITE_TRIPS +
            "   WHERE " + COLUMN_USER_ID + " = ?" +
            "   GROUP BY " + COLUMN_TRIP_ID + ", " + COLUMN_USER_ID +
            ")",
            new String[]{String.valueOf(userId)}
        );

        db.close();
    }
}

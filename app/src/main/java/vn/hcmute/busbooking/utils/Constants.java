package vn.hcmute.busbooking.utils;

public class Constants {
    // API URLs
    public static final String BASE_URL = "https://your-backend-api.herokuapp.com/api/";
    public static final String TRIPS_ENDPOINT = "trips";
    public static final String SEATS_ENDPOINT = "seats";
    public static final String PAYMENT_ENDPOINT = "payment";

    // Intent Keys
    public static final String KEY_TRIP = "trip";
    public static final String KEY_FROM = "from";
    public static final String KEY_TO = "to";
    public static final String KEY_DATE = "date";
    public static final String KEY_SELECTED_SEATS = "selected_seats";
    public static final String KEY_PICKUP_POINT = "pickup_point";
    public static final String KEY_DROPOFF_POINT = "dropoff_point";
    public static final String KEY_PASSENGER_INFO = "passenger_info";

    // Seat Types
    public static final String SEAT_TYPE_FLOOR1 = "floor1";
    public static final String SEAT_TYPE_FLOOR2 = "floor2";

    // Payment Methods
    public static final String PAYMENT_FUTAPAY = "FUTAPay";
    public static final String PAYMENT_ZALOPAY = "ZaloPay";
    public static final String PAYMENT_MOMO = "MoMo";
    public static final String PAYMENT_VISA = "Visa";

    // Bus Types
    public static final String BUS_TYPE_LIMOUSINE = "Limousine";
    public static final String BUS_TYPE_SLEEPER = "Giường nằm";
    public static final String BUS_TYPE_SEATER = "Ghế ngồi";

    // Time Constants
    public static final int SPLASH_DELAY = 2000; // 2 seconds
}
package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Seat; // Added Seat import

public interface ApiService {

    @GET("api/trips")
    Call<List<Trip>> getTrips(@Query("origin") String origin,
                              @Query("destination") String destination,
                              @Query("date") String date);

    @GET("api/routes")
    Call<List<Map<String, Object>>> getRoutes(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("q") String q
    );

    @GET("api/trips/{id}/seats")
    Call<List<Seat>> getSeats( // Changed to return List<Seat>
            @Path("id") int tripId,
            @Query("available") String available
    );

    @POST("api/bookings")
    Call<Map<String, Object>> createBooking(@Body Map<String, Object> body);

    @GET("api/bookings/my")
    Call<List<Booking>> getMyBookings(@Query("user_id") int userId);

    @POST("api/bookings/{id}/cancel")
    Call<Map<String, Object>> cancelBooking(@Path("id") int bookingId);

    @POST("api/auth/register")
    Call<Map<String, Object>> register(@Body Map<String, String> body);

    @POST("api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    @POST("api/auth/forgot-password")
    Call<Map<String, Object>> forgotPassword(@Body Map<String, String> body);

    @POST("api/auth/send-otp")
    Call<Map<String, Object>> sendOtp(@Body Map<String, String> body);

    @POST("api/auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @POST("api/auth/reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, String> body);

    @POST("api/auth/finish-register")
    Call<Map<String, Object>> finishRegister(@Body Map<String, String> body);

    @GET("api/promotions")
    Call<List<Map<String, Object>>> getPromotions();

    @GET("api/promotions/featured")
    Call<List<Map<String, Object>>> getFeaturedPromotions(@Query("limit") Integer limit);

    @POST("api/promotions/validate")
    Call<vn.hcmute.busbooking.model.PromotionValidateResponse> validatePromotion(@Body Map<String, Object> body);

    @GET("api/meta/locations")
    Call<Map<String, Object>> getMetaLocations();

    @GET("api/meta/operators")
    Call<List<String>> getOperators();

    @GET("api/trips/{id}")
    Call<Map<String, Object>> getTripDetails(@Path("id") int tripId);

    @GET("api/bus-image")
    Call<Map<String, Object>> getBusImage(
        @Query("operator") String operator,
        @Query("bus_type") String busType
    );

    @POST("api/bookings/{id}/payment")
    Call<Map<String, Object>> confirmPayment(@Path("id") int bookingId, @Body Map<String, String> body);

    @GET("api/bookings/{id}")
    Call<Map<String, Object>> getBookingDetails(@Path("id") int bookingId);

    @POST("api/bookings/{id}/verify-payment")
    Call<Map<String, Object>> verifyBookingPayment(@Path("id") int bookingId);

    @GET("api/auth/user/{id}")
    Call<Map<String, Object>> getUserInfo(@Path("id") int userId);

    @PUT("api/auth/user/{id}")
    Call<Map<String, Object>> updateUserInfo(@Path("id") int userId, @Body Map<String, String> body);

    @POST("api/auth/change-password")
    Call<Map<String, Object>> changePassword(@Body Map<String, String> body);

    @GET("api/popular")
    Call<List<Map<String, Object>>> getPopularRoutes(@Query("limit") Integer limit);

    @GET("api/reviews")
    Call<List<Map<String, Object>>> getReviews(@Query("limit") Integer limit);

    @POST("api/payment/payos/create")
    Call<vn.hcmute.busbooking.model.PaymentResponse> createPayosPayment(@Body vn.hcmute.busbooking.model.PaymentRequest request);

    @POST("api/payment/payos/verify")
    Call<Map<String, Object>> verifyPayos(@Body Map<String, String> body);

    @GET("api/trips/{id}/pickup-locations")
    Call<List<Location>> getPickupLocations(@Path("id") int tripId);

    @GET("api/trips/{id}/dropoff-locations")
    Call<List<Location>> getDropoffLocations(@Path("id") int tripId);
}

package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

import vn.hcmute.busbooking.model.Trip;

public interface ApiService {

    @GET("api/trips")
    Call<List<Trip>> getTrips(@Query("origin") String origin, @Query("destination") String destination);

    @GET("api/routes")
    Call<List<Map<String, Object>>> getRoutes(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("q") String q
    );

    @GET("api/trips/{id}/seats")
    Call<List<Map<String, Object>>> getSeats(
            @Path("id") int tripId,
            @Query("available") String available
    );

    @POST("api/bookings")
    Call<Map<String, Object>> createBooking(@Body Map<String, Object> body);

    @GET("api/bookings/my")
    Call<List<Map<String, Object>>> getMyBookings(@Query("user_id") int userId);

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

    // Promotions
    @GET("api/promotions")
    Call<List<Map<String, Object>>> getPromotions();

    @GET("api/promotions/featured")
    Call<List<Map<String, Object>>> getFeaturedPromotions(@Query("limit") Integer limit);

    // Meta locations
    @GET("api/meta/locations")
    Call<Map<String, Object>> getMetaLocations();

    // Payment
    @POST("api/bookings/{id}/payment")
    Call<Map<String, Object>> confirmPayment(@Path("id") int bookingId, @Body Map<String, String> body);

    // Booking details
    @GET("api/bookings/{id}")
    Call<Map<String, Object>> getBookingDetails(@Path("id") int bookingId);

    // User profile
    @GET("api/auth/user/{id}")
    Call<Map<String, Object>> getUserInfo(@Path("id") int userId);

    @PUT("api/auth/user/{id}")
    Call<Map<String, Object>> updateUserInfo(@Path("id") int userId, @Body Map<String, String> body);

    @POST("api/auth/change-password")
    Call<Map<String, Object>> changePassword(@Body Map<String, Object> body);
}

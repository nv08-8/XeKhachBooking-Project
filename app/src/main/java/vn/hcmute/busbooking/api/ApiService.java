package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.model.Booking; // Added

public interface ApiService {

    @GET("api/trips")
    Call<List<Trip>> getTrips(@Query("origin") String origin,
                              @Query("destination") String destination,
                              @Query("date") String date); // Added date param

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
    Call<List<Booking>> getMyBookings(@Query("user_id") int userId); // Changed to List<Booking>

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

    @GET("api/auth/user/{id}")
    Call<Map<String, Object>> getUserProfile(@Path("id") int userId);

    @PUT("api/auth/user/{id}")
    Call<Map<String, Object>> updateUserInfo(@Path("id") int userId, @Body Map<String, String> body);

    @PUT("api/auth/user/{id}")
    Call<Map<String, Object>> updateUserProfile(@Path("id") int userId, @Body Map<String, Object> body);

    @POST("api/auth/change-password")
    Call<Map<String, Object>> changePassword(@Body Map<String, String> body);

    // New: popular routes and reviews
    @GET("api/popular")
    Call<List<Map<String, Object>>> getPopularRoutes(@Query("limit") Integer limit);

    @GET("api/reviews")
    Call<List<Map<String, Object>>> getReviews(@Query("limit") Integer limit);

    // ----- Admin endpoints (FULL) -----

    // ========== ROUTES MANAGEMENT ==========
    @POST("api/admin/routes")
    Call<Map<String, Object>> createRoute(@Header("user-id") int userId, @Body Map<String, Object> body);

    @PUT("api/admin/routes/{id}")
    Call<Map<String, Object>> updateRoute(@Header("user-id") int userId, @Path("id") int routeId, @Body Map<String, Object> body);

    @DELETE("api/admin/routes/{id}")
    Call<Map<String, Object>> deleteRoute(@Header("user-id") int userId, @Path("id") int routeId);

    @GET("api/admin/routes/{id}")
    Call<Map<String, Object>> getRouteById(@Header("user-id") int userId, @Path("id") int routeId);

    // ========== TRIPS MANAGEMENT ==========
    @POST("api/admin/trips")
    Call<Map<String, Object>> createTrip(@Header("user-id") int userId, @Body Map<String, Object> body);

    @PUT("api/admin/trips/{id}")
    Call<Map<String, Object>> updateTrip(@Header("user-id") int userId, @Path("id") int tripId, @Body Map<String, Object> body);

    @DELETE("api/admin/trips/{id}")
    Call<Map<String, Object>> deleteTrip(@Header("user-id") int userId, @Path("id") int tripId);

    // ========== BOOKINGS MANAGEMENT ==========
    @GET("api/admin/bookings")
    Call<List<Map<String, Object>>> getAdminBookings(
            @Header("user-id") int userId,
            @Query("trip_id") String tripId,
            @Query("user_id") String userId2,
            @Query("status") String status,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @PUT("api/admin/bookings/{id}/confirm")
    Call<Map<String, Object>> confirmAdminBooking(@Header("user-id") int userId, @Path("id") int bookingId);

    @PUT("api/admin/bookings/{id}/cancel")
    Call<Map<String, Object>> cancelAdminBooking(@Header("user-id") int userId, @Path("id") int bookingId);

    // ========== REVENUE STATISTICS ==========
    @GET("api/admin/revenue/by-route")
    Call<List<Map<String, Object>>> getRevenueByRoute(@Header("user-id") int userId);

    @GET("api/admin/revenue/by-date")
    Call<List<Map<String, Object>>> getRevenueByDate(
            @Header("user-id") int userId,
            @Query("from_date") String fromDate,
            @Query("to_date") String toDate
    );

    @GET("api/admin/revenue/by-month")
    Call<List<Map<String, Object>>> getRevenueByMonth(@Header("user-id") int userId);

    @GET("api/admin/revenue/by-year")
    Call<List<Map<String, Object>>> getRevenueByYear(@Header("user-id") int userId);

}

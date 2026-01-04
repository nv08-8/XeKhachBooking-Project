package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.model.Driver;
import vn.hcmute.busbooking.model.Feedback;
import vn.hcmute.busbooking.model.Location;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.model.User;

public interface ApiService {

    @GET("api/trips")
    Call<List<Map<String, Object>>> getTrips(@Query("route_id") Integer routeId,
                                            @Query("origin") String origin,
                                            @Query("destination") String destination,
                                            @Query("date") String date);

    @GET("api/routes")
    Call<List<Map<String, Object>>> getRoutes(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("q") String q
    );

    @GET("api/trips/{id}/seats")
    Call<List<Seat>> getSeats(@Path("id") int tripId);

    @GET("api/trips/{id}/seats")
    Call<List<Seat>> getSeats( // Changed to return List<Seat>
            @Path("id") int tripId,
            @Query("available") String available
    );

    @GET("api/trips/{id}/seats")
    Call<List<Map<String, Object>>> getTripSeatsAvailable(@Path("id") int tripId, @Query("available") boolean available);

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

    @GET("api/meta/bus-types")
    Call<List<String>> getBusTypes();

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

    @PUT("api/bookings/{id}/payment-method")
    Call<Map<String, Object>> changePaymentMethod(@Path("id") int bookingId, @Body Map<String, String> body);

    @POST("api/bookings/{id}/confirm-offline-payment")
    Call<Map<String, Object>> confirmOfflinePayment(@Path("id") int bookingId);

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

    // ----- User Favorites -----
    @GET("api/favorites")
    Call<List<Trip>> getFavorites(@Header("user-id") int userId);

    @POST("api/favorites")
    Call<Map<String, Object>> addFavorite(@Header("user-id") int userId, @Body Map<String, Integer> body);

    @DELETE("api/favorites/{trip_id}")
    Call<Map<String, Object>> removeFavorite(@Header("user-id") int userId, @Path("trip_id") int tripId);


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
    @GET("api/admin/trips/{id}")
    Call<Map<String, Object>> getAdminTripDetails(@Header("user-id") int userId, @Path("id") int tripId);

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

    @POST("api/admin/bookings")
    Call<Map<String, Object>> adminCreateBooking(@Header("user-id") int userId, @Body Map<String, Object> body);

    // ========== USER MANAGEMENT ==========
    @GET("api/admin/users")
    Call<List<User>> getAllUsers(@Header("user-id") int userId);

    @PUT("api/admin/users/{id}")
    Call<User> updateUser(@Header("user-id") int adminId, @Path("id") int userId, @Body User user);

    @DELETE("api/admin/users/{id}")
    Call<Void> deleteUser(@Header("user-id") int adminId, @Path("id") int userId);

    // ========== REVENUE STATISTICS ==========
    @GET("api/admin/revenue")
    Call<List<Map<String, Object>>> getRevenue(
            @Header("user-id") int userId,
            @Query("groupBy") String groupBy,
            @Query("route_id") Integer routeId,
            @Query("trip_id") Integer tripId,
            @Query("from_date") String fromDate,
            @Query("to_date") String toDate,
            @Query("payment_method") String paymentMethod
    );

    // Báo cáo hoàn tiền
    @GET("api/admin/revenue/refunds")
    Call<List<Map<String, Object>>> getRevenueRefunds(
            @Header("user-id") int userId,
            @Query("groupBy") String groupBy,
            @Query("route_id") Integer routeId,
            @Query("trip_id") Integer tripId,
            @Query("from_date") String fromDate,
            @Query("to_date") String toDate,
            @Query("refundType") String refundType
    );

    @GET("api/admin/revenue/details")
    Call<List<Map<String, Object>>> getRevenueDetails(
        @Header("user-id") int userId,
        @Query("group_by") String groupBy,
        @Query("value") String value,
        @Query("payment_method") String paymentMethod
    );

    // Chi tiết hoàn tiền
    @GET("api/admin/revenue/refund-details")
    Call<List<Map<String, Object>>> getRevenueRefundDetails(
        @Header("user-id") int userId,
        @Query("group_by") String groupBy,
        @Query("value") String value,
        @Query("refundType") String refundType,
        @Query("payment_method") String paymentMethod
    );


    // ========== DRIVERS MANAGEMENT ==========
    @GET("api/admin/drivers")
    Call<List<Driver>> getAllDrivers(@Header("user-id") int userId);

    @POST("api/admin/drivers")
    Call<Driver> createDriver(@Header("user-id") int userId, @Body Map<String, String> body);

    @GET("api/admin/drivers/{id}")
    Call<Driver> getDriverById(@Header("user-id") int userId, @Path("id") int driverId);

    @PUT("api/admin/drivers/{id}")
    Call<Driver> updateDriver(@Header("user-id") int userId, @Path("id") int driverId, @Body Map<String, String> body);

    @DELETE("api/admin/drivers/{id}")
    Call<Void> deleteDriver(@Header("user-id") int userId, @Path("id") int driverId);

    // ========== PROMOTIONS MANAGEMENT ==========
    @GET("api/admin/promotions")
    Call<List<Promotion>> getAdminPromotions(@Header("user-id") int userId);

    @POST("api/admin/promotions")
    Call<Promotion> createPromotion(@Header("user-id") int userId, @Body Promotion promotion);

    @PUT("api/admin/promotions/{id}")
    Call<Promotion> updatePromotion(@Header("user-id") int userId, @Path("id") int promotionId, @Body Promotion promotion);

    @GET("api/admin/promotions/{id}")
    Call<Promotion> getPromotionById(@Header("user-id") int userId, @Path("id") int promotionId);

    @DELETE("api/admin/promotions/{id}")
    Call<Void> deletePromotion(@Header("user-id") int userId, @Path("id") int promotionId);

    // ========== OFFLINE PAYMENT CONFIRMATION ==========
    @GET("api/admin/pending-offline-payments")
    Call<List<Map<String, Object>>> getPendingOfflinePayments(@Header("user-id") int userId);

    @POST("api/admin/confirm-offline-payment/{id}")
    Call<Map<String, Object>> adminConfirmOfflinePayment(@Header("user-id") int userId, @Path("id") int bookingId);

    // ========== USER AVATAR UPLOAD ==========
    @POST("api/auth/upload-avatar")
    @Multipart
    Call<Map<String, Object>> uploadUserAvatar(
            @Header("user-id") int userId,
            @Part MultipartBody.Part avatar
    );

    // ========== BOOKING CONFIRMATION EMAIL ==========
    @POST("api/bookings/{id}/send-confirmation-email")
    Call<Map<String, Object>> sendPaymentConfirmationEmail(@Path("id") int bookingId);

    // ========== FEEDBACK MANAGEMENT ==========
    @GET("api/feedbacks/pending")
    Call<List<Feedback>> getPendingFeedbacks(@Query("user_id") int userId);

    @GET("api/feedbacks/reviewed")
    Call<List<Feedback>> getReviewedFeedbacks(@Query("user_id") int userId);

    @POST("api/feedbacks")
    Call<Feedback> submitFeedback(@Body Map<String, Object> body);

}

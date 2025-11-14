package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import vn.hcmute.busbooking.model.Trip;

public interface ApiService {

    @GET("api/trips")
    Call<List<Trip>> getTrips();

    @POST("api/auth/finish-register")
    Call<Map<String, Object>> finishRegister(@Body Map<String, String> body);

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
}

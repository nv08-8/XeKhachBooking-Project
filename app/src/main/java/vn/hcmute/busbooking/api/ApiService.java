package vn.hcmute.busbooking.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import vn.hcmute.busbooking.model.Trip;

public interface ApiService {
    @GET("trips")
    Call<List<Trip>> getTrips();

    @POST("auth/register")
    Call<Map<String, Object>> register(@Body Map<String, String> body);

    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    @POST("auth/forgot-password")
    Call<Map<String, Object>> forgotPassword(@Body Map<String, String> body);

    @POST("auth/send-otp")
    Call<Map<String, Object>> sendOtp(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @POST("auth/reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, String> body);

    @POST("auth/finish-register")
    Call<Map<String, Object>> finishRegister(@Body Map<String, String> body);

}

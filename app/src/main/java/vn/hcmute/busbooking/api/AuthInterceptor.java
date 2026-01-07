package vn.hcmute.busbooking.api;

import android.content.Context;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import vn.hcmute.busbooking.utils.SessionManager;
import java.io.IOException;

/**
 * Interceptor for adding JWT token to all API requests
 */
public class AuthInterceptor implements Interceptor {
    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Get token from SessionManager
        SessionManager sessionManager = new SessionManager(context);
        String token = sessionManager.getToken();

        Request.Builder requestBuilder = originalRequest.newBuilder();

        // Add Authorization header if token exists
        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}


package vn.hcmute.busbooking.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import vn.hcmute.busbooking.MainActivity;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.guest.GuestHomeActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        // Chờ 2 giây rồi chuyển sang màn hình chính hoặc màn hình khách
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            try {
                Integer userId = sessionManager.getUserId();

                if (userId != null) {
                    // User is logged in, go to MainActivity
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // No user logged in, go to GuestHomeActivity
                    intent = new Intent(SplashActivity.this, GuestHomeActivity.class);
                }
            } catch (Exception e) {
                // If session data is corrupted, clear it and go to guest mode
                android.util.Log.e("SplashActivity", "Error reading session, clearing...", e);
                sessionManager.clearSession();
                intent = new Intent(SplashActivity.this, GuestHomeActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}

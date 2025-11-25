package vn.hcmute.busbooking.ws;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class BookingWebSocketClient {
    private static final String TAG = "BookingWS";
    private Socket socket;
    private Activity activity;
    private String serverUrl; // e.g. https://api.example.com
    private String token;

    public BookingWebSocketClient(Activity activity, String serverUrl, String token) {
        this.activity = activity;
        this.serverUrl = serverUrl;
        this.token = token;
    }

    public void connect() {
        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;
            socket = IO.socket(serverUrl, opts);

            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "socket connected"));

            socket.on("booking_event", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        Object payload = args[0];
                        try {
                            JSONObject obj = new JSONObject(payload.toString());
                            String status = obj.optString("status", "");
                            if ("expired".equals(status)) {
                                showExpiredBanner();
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "Invalid payload", e);
                        }
                    }
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "socket disconnected"));
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket URL", e);
        }
    }

    private void showExpiredBanner() {
        activity.runOnUiThread(() -> {
            Snackbar.make(activity.findViewById(android.R.id.content), "Vé chờ thanh toán đã hết hạn", Snackbar.LENGTH_LONG).show();
            try {
                activity.getClass().getMethod("refreshBookings").invoke(activity);
            } catch (Exception ignored) {
            }
        });
    }

    public void close() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
    }
}

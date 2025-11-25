package vn.hcmute.busbooking.ws;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.socket.client.IO;
import io.socket.client.Socket;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.utils.SessionManager;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private final Context ctx;
    private final Set<BookingEventListener> listeners = Collections.synchronizedSet(new HashSet<>());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BookingEventListener {
        void onBookingExpired(JSONObject payload);
    }

    private SocketManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static synchronized SocketManager getInstance(Context ctx) {
        if (instance == null) instance = new SocketManager(ctx);
        return instance;
    }

    public synchronized void connect() {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Socket already connected");
            return;
        }

        String token = new SessionManager(ctx).getToken();
        if (token == null) {
            Log.d(TAG, "No token available, not connecting socket");
            return;
        }

        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;
            String url = ApiClient.getBaseUrl();
            socket = IO.socket(url, opts);

            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "socket connected"));

            socket.on("booking_event", args -> {
                if (args.length > 0) {
                    Object payload = args[0];
                    try {
                        JSONObject obj = new JSONObject(payload.toString());
                        // notify listeners on main thread
                        mainHandler.post(() -> {
                            synchronized (listeners) {
                                for (BookingEventListener l : listeners) {
                                    try { l.onBookingExpired(obj); } catch (Exception ignored) {}
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.w(TAG, "Invalid booking_event payload", e);
                    }
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "socket disconnected"));
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket URL", e);
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            try {
                socket.disconnect();
                socket.close();
            } catch (Exception ignored) {}
            socket = null;
        }
        listeners.clear();
    }

    public void addBookingListener(BookingEventListener l) {
        if (l == null) return;
        listeners.add(l);
    }

    public void removeBookingListener(BookingEventListener l) {
        if (l == null) return;
        listeners.remove(l);
    }
}

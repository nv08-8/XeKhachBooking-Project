package vn.hcmute.busbooking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.GuestHomeActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin, tvPromoBanner;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views and SessionManager
        etOrigin = view.findViewById(R.id.etOrigin);
        etDestination = view.findViewById(R.id.etDestination);
        btnSearchTrips = view.findViewById(R.id.btnSearchTrips);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvLogin = view.findViewById(R.id.tvLogin);
        // tvPromoBanner might be null if not in this layout variant
        try {
            tvPromoBanner = view.findViewById(R.id.tvPromoBanner);
        } catch (Exception e) {
            tvPromoBanner = null;
        }
        sessionManager = new SessionManager(getContext());
        apiService = ApiClient.getClient().create(ApiService.class);

        // Update UI based on login status
        updateUI();

        // Fetch locations from backend
        fetchMetaLocations();

        // Fetch and display featured promotions
        fetchFeaturedPromotions();

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString();
            String to = etDestination.getText().toString();

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn điểm đi và điểm đến", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getActivity(), TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            startActivity(intent);
        });
    }

    private void fetchMetaLocations() {
        apiService.getMetaLocations().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && getContext() != null) {
                    Map<String, Object> meta = response.body();
                    List<String> origins = (List<String>) meta.get("origins");
                    List<String> destinations = (List<String>) meta.get("destinations");

                    if (origins != null && !origins.isEmpty()) {
                        ArrayAdapter<String> originAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_list_item_1, origins);
                        etOrigin.setAdapter(originAdapter);
                    }

                    if (destinations != null && !destinations.isEmpty()) {
                        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_list_item_1, destinations);
                        etDestination.setAdapter(destAdapter);
                    }

                    Log.d(TAG, "Meta locations loaded: " + origins.size() + " origins, " + destinations.size() + " destinations");
                } else {
                    // Fallback to hardcoded locations
                    loadFallbackLocations();
                    Log.w(TAG, "Failed to load meta locations, using fallback");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error fetching meta locations: " + t.getMessage());
                loadFallbackLocations();
            }
        });
    }

    private void loadFallbackLocations() {
        if (getContext() == null) return;
        String[] locations = {"TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Nha Trang", "Huế", "Cần Thơ"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);
    }

    private void fetchFeaturedPromotions() {
        apiService.getFeaturedPromotions(3).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty() && tvPromoBanner != null) {
                    List<Map<String, Object>> promos = response.body();
                    Map<String, Object> firstPromo = promos.get(0);
                    String title = (String) firstPromo.get("title");
                    Object discountObj = firstPromo.get("discount_percent");
                    String discount = discountObj != null ? String.valueOf(discountObj) : "0";

                    tvPromoBanner.setText("🎉 " + title + " - Giảm " + discount + "%");
                    tvPromoBanner.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Featured promotions loaded: " + promos.size());
                } else {
                    if (tvPromoBanner != null) tvPromoBanner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Error fetching promotions: " + t.getMessage());
                if (tvPromoBanner != null) tvPromoBanner.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        String userName = sessionManager.getUserName();
        // Check if user is logged in (userName is not null and not "null")
        if (userName != null && !userName.equalsIgnoreCase("null")) {
            // User is logged in
            tvWelcome.setText("Xin chào, " + userName + "!");
            tvLogin.setText("Đăng xuất");
            tvLogin.setOnClickListener(v -> {
                // Perform logout
                sessionManager.clearSession();
                Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

                // Go to GuestHomeActivity
                Intent intent = new Intent(getActivity(), GuestHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });
        } else {
            // User is not logged in (Guest mode - although this fragment is for logged in users)
            tvWelcome.setText("Xin chào, Khách!");
            tvLogin.setText("Đăng nhập");
            tvLogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }
    }
}

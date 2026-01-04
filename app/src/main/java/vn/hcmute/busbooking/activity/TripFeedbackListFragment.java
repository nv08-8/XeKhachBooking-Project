package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.TripFeedbackAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripFeedbackListFragment extends Fragment implements TripFeedbackAdapter.OnTripClickListener {

    private RecyclerView rvTrips;
    private TripFeedbackAdapter adapter;
    private List<Map<String, Object>> tripList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmptyMessage;
    private ApiService apiService;
    private SessionManager sessionManager;
    private OnTripSelectedListener tripSelectedListener;

    public interface OnTripSelectedListener {
        void onTripSelected(int tripId);
    }

    public void setOnTripSelectedListener(OnTripSelectedListener listener) {
        this.tripSelectedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_feedback_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(getContext());
        apiService = ApiClient.getClient().create(ApiService.class);

        // Setup Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        // Find Views
        rvTrips = view.findViewById(R.id.rvTrips);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        // Setup RecyclerView
        rvTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TripFeedbackAdapter(tripList, this);
        rvTrips.setAdapter(adapter);

        // Load trips with feedback
        loadTripsWithFeedback();
    }

    private void loadTripsWithFeedback() {
        progressBar.setVisibility(View.VISIBLE);
        rvTrips.setVisibility(View.GONE);
        tvEmptyMessage.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();

        // ✅ Gọi API để lấy tất cả feedback + trip info
        apiService.getTripsWithFeedback(userId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> allFeedbacks = response.body();

                    // ✅ Group feedbacks theo trip_id để tạo danh sách trips
                    Map<Integer, Map<String, Object>> tripMap = new HashMap<>();

                    for (Map<String, Object> feedback : allFeedbacks) {
                        Object tripIdObj = feedback.get("trip_id");
                        if (tripIdObj != null) {
                            int tripId = Integer.parseInt(tripIdObj.toString());

                            // Nếu trip chưa có trong map, thêm vào
                            if (!tripMap.containsKey(tripId)) {
                                Map<String, Object> trip = new HashMap<>();
                                trip.put("id", tripId);
                                trip.put("departure_time", feedback.get("departure_time"));
                                trip.put("arrival_time", feedback.get("arrival_time"));
                                trip.put("operator", feedback.get("operator"));
                                trip.put("bus_type", feedback.get("bus_type"));
                                trip.put("origin", feedback.get("origin"));
                                trip.put("destination", feedback.get("destination"));
                                trip.put("feedback_count", feedback.get("trip_feedback_count"));
                                tripMap.put(tripId, trip);
                            }
                        }
                    }

                    tripList.clear();
                    tripList.addAll(tripMap.values());
                    adapter.notifyDataSetChanged();

                    progressBar.setVisibility(View.GONE);
                    if (tripList.isEmpty()) {
                        tvEmptyMessage.setText("Chưa có chuyến nào có feedback");
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                    } else {
                        rvTrips.setVisibility(View.VISIBLE);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyMessage.setText("Không thể tải dữ liệu");
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyMessage.setText("Lỗi: " + t.getMessage());
                tvEmptyMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onTripClick(int tripId) {
        // Gọi callback để thông báo Activity rằng đã chọn chuyến
        if (tripSelectedListener != null) {
            tripSelectedListener.onTripSelected(tripId);
        }
    }
}


package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.TripFeedbackDetailAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Feedback;

public class TripFeedbackDetailsFragment extends Fragment {

    private RecyclerView rvFeedbacks;
    private TripFeedbackDetailAdapter adapter;
    private List<Feedback> feedbackList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmptyMessage;
    private TextView tvTripInfo;
    private ApiService apiService;
    private int tripId;
    private OnBackPressedListener backListener;

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    public void setBackPressedListener(OnBackPressedListener listener) {
        this.backListener = listener;
    }

    public static TripFeedbackDetailsFragment newInstance(int tripId) {
        TripFeedbackDetailsFragment fragment = new TripFeedbackDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("trip_id", tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tripId = getArguments().getInt("trip_id", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_feedback_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        // Setup Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (backListener != null) {
                backListener.onBackPressed();
            }
        });

        // Find Views
        rvFeedbacks = view.findViewById(R.id.rvFeedbacks);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvTripInfo = view.findViewById(R.id.tvTripInfo);

        // Setup RecyclerView
        rvFeedbacks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TripFeedbackDetailAdapter(feedbackList);

        // ✅ Set delete listener
        adapter.setDeleteListener((feedbackId, position) -> {
            showDeleteConfirmDialog(feedbackId, position);
        });

        rvFeedbacks.setAdapter(adapter);

        // Load feedback for this trip
        loadTripFeedbacks();
    }

    // ✅ Hiển thị dialog xác nhận xóa
    private void showDeleteConfirmDialog(int feedbackId, int position) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Xóa feedback")
                .setMessage("Bạn có chắc chắn muốn xóa feedback này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteFeedback(feedbackId, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ✅ Xóa feedback qua API
    private void deleteFeedback(int feedbackId, int position) {
        apiService.deleteFeedback(feedbackId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Xóa khỏi list
                    if (position >= 0 && position < feedbackList.size()) {
                        feedbackList.remove(position);
                        adapter.notifyItemRemoved(position);
                    }

                    // Cập nhật count
                    if (feedbackList.isEmpty()) {
                        tvEmptyMessage.setText("Chuyến này chưa có feedback nào");
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        rvFeedbacks.setVisibility(View.GONE);
                    } else {
                        tvTripInfo.setText("Feedback (" + feedbackList.size() + ")");
                    }

                    android.widget.Toast.makeText(getContext(), "Xóa feedback thành công", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(getContext(), "Xóa feedback thất bại", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.widget.Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTripFeedbacks() {
        progressBar.setVisibility(View.VISIBLE);
        rvFeedbacks.setVisibility(View.GONE);
        tvEmptyMessage.setVisibility(View.GONE);

        // Gọi API lấy feedback của chuyến này
        apiService.getTripFeedbacks(tripId).enqueue(new Callback<List<Feedback>>() {
            @Override
            public void onResponse(Call<List<Feedback>> call, Response<List<Feedback>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    feedbackList.clear();
                    feedbackList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (feedbackList.isEmpty()) {
                        tvEmptyMessage.setText("Chuyến này chưa có feedback nào");
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                    } else {
                        tvTripInfo.setText("Feedback (" + feedbackList.size() + ")");
                        rvFeedbacks.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyMessage.setText("Không thể tải dữ liệu");
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Feedback>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyMessage.setText("Lỗi: " + t.getMessage());
                tvEmptyMessage.setVisibility(View.VISIBLE);
            }
        });
    }
}


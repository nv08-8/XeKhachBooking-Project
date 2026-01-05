package vn.hcmute.busbooking.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.FeedbackAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Feedback;
import vn.hcmute.busbooking.utils.SessionManager;

public class FeedbackManagementActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_feedback_management);

        SessionManager sessionManager = new SessionManager(this);
        Integer id = sessionManager.getUserId();
        userId = (id != null) ? id : -1;

        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem nhận xét", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup ViewPager2 và TabLayout
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Setup toolbar navigation
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        FeedbackPagerAdapter adapter = new FeedbackPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Chưa đánh giá" : "Đã đánh giá");
        }).attach();

        // Handle window insets
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        View statusBarScrim = findViewById(R.id.statusBarScrim);
        handleWindowInsets(appBarLayout, statusBarScrim);
    }

    private void handleWindowInsets(AppBarLayout appBarLayout, View statusBarScrim) {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            if (statusBarScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                scrimParams.height = statusBarHeight;
                statusBarScrim.setLayoutParams(scrimParams);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
            }

            return insets;
        });
    }

    private class FeedbackPagerAdapter extends FragmentStateAdapter {
        public FeedbackPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return FeedbackListFragment.newInstance(position, userId);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public static class FeedbackListFragment extends androidx.fragment.app.Fragment implements FeedbackAdapter.OnFeedbackClickListener {
        private int type;
        private int userId;
        private RecyclerView recyclerView;
        private FeedbackAdapter adapter;
        private List<Feedback> feedbackList = new ArrayList<>();
        private ApiService apiService;

        public static FeedbackListFragment newInstance(int type, int userId) {
            FeedbackListFragment fragment = new FeedbackListFragment();
            Bundle args = new Bundle();
            args.putInt("type", type);
            args.putInt("userId", userId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_booking_list, container, false);
            type = getArguments().getInt("type");
            userId = getArguments().getInt("userId");
            recyclerView = view.findViewById(R.id.rvBookings);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            apiService = ApiClient.getClient().create(ApiService.class);
            adapter = new FeedbackAdapter(feedbackList, type, this);
            recyclerView.setAdapter(adapter);

            loadFeedbacks();
            return view;
        }

        private void loadFeedbacks() {
            Call<List<Feedback>> call = (type == 0) ? 
                apiService.getPendingFeedbacks(userId) : 
                apiService.getReviewedFeedbacks(userId);

            call.enqueue(new Callback<List<Feedback>>() {
                @Override
                public void onResponse(Call<List<Feedback>> call, Response<List<Feedback>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        feedbackList.clear();
                        feedbackList.addAll(response.body());
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<List<Feedback>> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onFeedbackClick(Feedback feedback) {
            showFeedbackDialog(feedback);
        }

        private void showFeedbackDialog(Feedback feedback) {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback_submit, null);
            RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
            EditText etComment = dialogView.findViewById(R.id.etComment);

            new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    String comment = etComment.getText().toString();
                    if (rating == 0) {
                        Toast.makeText(getContext(), "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitFeedback(feedback.getBookingId(), rating, comment);
                })
                .setNegativeButton("Hủy", null)
                .show();
        }

        private void submitFeedback(int bookingId, int rating, String comment) {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("booking_id", bookingId);
            body.put("rating", rating);
            body.put("comment", comment);

            apiService.submitFeedback(body).enqueue(new Callback<Feedback>() {
                @Override
                public void onResponse(Call<Feedback> call, Response<Feedback> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Gửi nhận xét thành công", Toast.LENGTH_SHORT).show();
                        loadFeedbacks();
                    } else {
                        Toast.makeText(getContext(), "Gửi nhận xét thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Feedback> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.FeedbackDetailAdapter;

public class OperatorRatingsActivity extends AppCompatActivity {

    private List<Map<String, Object>> feedbacks;
    private double averageRating;
    private int totalRatings;

    public static final String EXTRA_FEEDBACKS = "feedbacks";
    public static final String EXTRA_AVERAGE_RATING = "averageRating";
    public static final String EXTRA_TOTAL_RATINGS = "totalRatings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.dialog_operator_ratings);

        // Handle window insets
        handleWindowInsets();

        // Get data from intent
        if (getIntent() != null) {
            feedbacks = (List<Map<String, Object>>) getIntent().getSerializableExtra(EXTRA_FEEDBACKS);
            averageRating = getIntent().getDoubleExtra(EXTRA_AVERAGE_RATING, 0.0);
            totalRatings = getIntent().getIntExtra(EXTRA_TOTAL_RATINGS, 0);
        }

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Set average rating
        TextView txtAverageRating = findViewById(R.id.txtAverageRating);
        if (txtAverageRating != null) {
            txtAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
        }

        // Set total reviews
        TextView txtTotalReviews = findViewById(R.id.txtTotalReviews);
        if (txtTotalReviews != null) {
            txtTotalReviews.setText(String.format(Locale.getDefault(), "(%d đánh giá)", totalRatings));
        }

        // Calculate rating distribution
        int[] ratingCounts = new int[5];
        if (feedbacks != null && !feedbacks.isEmpty()) {
            for (Map<String, Object> feedback : feedbacks) {
                Object ratingObj = feedback.get("rating");
                if (ratingObj instanceof Number) {
                    int rating = ((Number) ratingObj).intValue();
                    if (rating >= 1 && rating <= 5) {
                        ratingCounts[rating - 1]++;
                    }
                }
            }
        }

        // Update progress bars
        updateProgressBar(R.id.progressBar5, R.id.txtPercent5, ratingCounts[4]);
        updateProgressBar(R.id.progressBar4, R.id.txtPercent4, ratingCounts[3]);
        updateProgressBar(R.id.progressBar3, R.id.txtPercent3, ratingCounts[2]);

        // Set up RecyclerView
        RecyclerView rvFeedbacks = findViewById(R.id.rvFeedbacks);
        TextView tvNoFeedbacks = findViewById(R.id.tvNoFeedbacks);

        if (feedbacks != null && !feedbacks.isEmpty()) {
            if (rvFeedbacks != null) {
                rvFeedbacks.setVisibility(View.VISIBLE);
                rvFeedbacks.setLayoutManager(new LinearLayoutManager(this));
                rvFeedbacks.setAdapter(new FeedbackDetailAdapter(this, feedbacks));
            }
            if (tvNoFeedbacks != null) {
                tvNoFeedbacks.setVisibility(View.GONE);
            }
        } else {
            if (rvFeedbacks != null) {
                rvFeedbacks.setVisibility(View.GONE);
            }
            if (tvNoFeedbacks != null) {
                tvNoFeedbacks.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateProgressBar(int progressBarId, int textViewId, int count) {
        ProgressBar progressBar = findViewById(progressBarId);
        TextView textView = findViewById(textViewId);

        if (progressBar != null && textView != null) {
            int percent = totalRatings > 0 ? (count * 100) / totalRatings : 0;
            progressBar.setProgress(percent);
            textView.setText(String.valueOf(percent) + "%");
        }
    }

    private void handleWindowInsets() {
        View statusBarScrim = findViewById(R.id.statusBarScrim);
        View root = getWindow().getDecorView();

        if (statusBarScrim != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

                // Set statusBarScrim height to match status bar height
                ViewGroup.LayoutParams layoutParams = statusBarScrim.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.height = statusBarHeight;
                    statusBarScrim.setLayoutParams(layoutParams);
                }

                return insets;
            });
        }
    }
}


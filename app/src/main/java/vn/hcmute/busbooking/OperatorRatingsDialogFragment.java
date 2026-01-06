package vn.hcmute.busbooking;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.FeedbackDetailAdapter;

public class OperatorRatingsDialogFragment extends DialogFragment {

    private List<Map<String, Object>> feedbacks;
    private double averageRating;
    private int totalRatings;

    public OperatorRatingsDialogFragment(List<Map<String, Object>> feedbacks, double averageRating, int totalRatings) {
        this.feedbacks = feedbacks;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_operator_ratings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar close button
        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> dismiss());
        }

        // Set average rating
        TextView txtAverageRating = view.findViewById(R.id.txtAverageRating);
        txtAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));

        // Set total reviews
        TextView txtTotalReviews = view.findViewById(R.id.txtTotalReviews);
        txtTotalReviews.setText(String.format(Locale.getDefault(), "(%d đánh giá)", totalRatings));


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
        updateProgressBar(view, R.id.progressBar5, R.id.txtPercent5, ratingCounts[4]);
        updateProgressBar(view, R.id.progressBar4, R.id.txtPercent4, ratingCounts[3]);
        updateProgressBar(view, R.id.progressBar3, R.id.txtPercent3, ratingCounts[2]);

        // Set up RecyclerView
        RecyclerView rvFeedbacks = view.findViewById(R.id.rvFeedbacks);
        TextView tvNoFeedbacks = view.findViewById(R.id.tvNoFeedbacks);

        if (feedbacks != null && !feedbacks.isEmpty()) {
            rvFeedbacks.setVisibility(View.VISIBLE);
            tvNoFeedbacks.setVisibility(View.GONE);
            rvFeedbacks.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFeedbacks.setAdapter(new FeedbackDetailAdapter(requireContext(), feedbacks));
        } else {
            rvFeedbacks.setVisibility(View.GONE);
            tvNoFeedbacks.setVisibility(View.VISIBLE);
        }
    }

    private void updateProgressBar(View view, int progressBarId, int textViewId, int count) {
        ProgressBar progressBar = view.findViewById(progressBarId);
        TextView textView = view.findViewById(textViewId);

        int percent = totalRatings > 0 ? (count * 100) / totalRatings : 0;
        progressBar.setProgress(percent);
        textView.setText(percent + "%");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set dialog to full width
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}


package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class FeedbackDetailAdapter extends RecyclerView.Adapter<FeedbackDetailAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> feedbacks;

    public FeedbackDetailAdapter(Context context, List<Map<String, Object>> feedbacks) {
        this.context = context;
        this.feedbacks = feedbacks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feedback_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> feedback = feedbacks.get(position);

        // User name
        Object userNameObj = feedback.get("user_name");
        if (userNameObj != null) {
            holder.tvUserName.setText(userNameObj.toString());
        }

        // Rating
        Object ratingObj = feedback.get("rating");
        float rating = 0;
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).floatValue();
            holder.tvRatingNumber.setText(String.format(Locale.getDefault(), "%.1f", rating));

            // Display stars as emoji
            int starCount = Math.round(rating);
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < starCount; i++) {
                stars.append("⭐");
            }
            holder.tvStarRating.setText(stars.toString());
        }

        // Comment
        Object commentObj = feedback.get("comment");
        if (commentObj != null && !commentObj.toString().isEmpty()) {
            holder.tvComment.setText(commentObj.toString());
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // Date
        Object dateObj = feedback.get("created_at");
        if (dateObj != null) {
            holder.tvDate.setText(formatDate(dateObj.toString()));
        }
    }

    @Override
    public int getItemCount() {
        return feedbacks != null ? feedbacks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        TextView tvStarRating;
        TextView tvRatingNumber;
        TextView tvComment;
        TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvStarRating = itemView.findViewById(R.id.tvStarRating);
            tvRatingNumber = itemView.findViewById(R.id.tvRatingNumber);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    private String formatDate(String isoDateString) {
        try {
            // Parse ISO format: "2026-01-06T10:30:00Z"
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(isoDateString);

            // Format to Vietnamese: "6 tháng 1, 2026"
            SimpleDateFormat viFormat = new SimpleDateFormat("d 'tháng' M, yyyy", new Locale("vi", "VN"));
            return viFormat.format(date);
        } catch (ParseException e) {
            return isoDateString;
        }
    }
}


package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Feedback;

public class TripFeedbackDetailAdapter extends RecyclerView.Adapter<TripFeedbackDetailAdapter.FeedbackViewHolder> {

    private List<Feedback> feedbackList;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDeleteFeedback(int feedbackId, int position);
    }

    public TripFeedbackDetailAdapter(List<Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    public void setDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_feedback_detail, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        Feedback feedback = feedbackList.get(position);

        holder.tvUserName.setText(feedback.getUserName() != null ? feedback.getUserName() : "Khách");
        holder.tvComment.setText(feedback.getComment() != null ? feedback.getComment() : "Không có bình luận");
        holder.ratingBar.setRating(feedback.getRating());
        holder.ratingBar.setIsIndicator(true);

        // Format thời gian feedback
        String formattedDate = formatDateTime(feedback.getFeedbackDate() != null ? feedback.getFeedbackDate() : "");
        holder.tvDate.setText(formattedDate);

        // Delete button handler
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteFeedback(feedback.getId(), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    // ✅ Format ISO timestamp thành format đẹp: HH:mm dd/MM/yyyy (UTC+7)
    private String formatDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isEmpty()) {
            return "";
        }
        try {
            // Parse ISO format: 2026-01-04T14:23:41.427Z (UTC)
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoDateTime);

            // Format thành: HH:mm dd/MM/yyyy (Vietnam timezone UTC+7)
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US);
            outputFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Nếu parse lỗi, trả về thời gian gốc
            return isoDateTime;
        }
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvComment, tvDate;
        RatingBar ratingBar;
        ImageButton btnDelete;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}


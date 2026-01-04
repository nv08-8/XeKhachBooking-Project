package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Feedback;

public class FeedbackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PENDING = 0;
    private static final int TYPE_REVIEWED = 1;

    private List<Feedback> feedbackList;
    private int type;
    private OnFeedbackClickListener listener;

    public interface OnFeedbackClickListener {
        void onFeedbackClick(Feedback feedback);
    }

    public FeedbackAdapter(List<Feedback> feedbackList, int type, OnFeedbackClickListener listener) {
        this.feedbackList = feedbackList;
        this.type = type;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (type == TYPE_PENDING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_pending, parent, false);
            return new PendingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_reviewed, parent, false);
            return new ReviewedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Feedback feedback = feedbackList.get(position);

        if (holder instanceof PendingViewHolder) {
            PendingViewHolder vh = (PendingViewHolder) holder;
            vh.tvOperator.setText(feedback.getOperator() != null ? feedback.getOperator() : "");
            vh.tvVehicleType.setText(feedback.getVehicleType() != null ? feedback.getVehicleType() : "");
            vh.tvPrice.setText(feedback.getPrice() != null ? feedback.getPrice() : "");
            vh.tvDepartureTime.setText(formatTime(feedback.getDepartureTime()));
            vh.tvOrigin.setText(feedback.getOrigin() != null ? feedback.getOrigin() : "");
            vh.tvArrivalTime.setText(formatTime(feedback.getArrivalTime()));
            vh.tvDestination.setText(feedback.getDestination() != null ? feedback.getDestination() : "");
            vh.tvDuration.setText(feedback.getDuration() != null ? feedback.getDuration() : "");
            vh.tvDate.setText(feedback.getDate() != null ? feedback.getDate() : "");
            vh.btnFeedback.setOnClickListener(v -> listener.onFeedbackClick(feedback));
        } else if (holder instanceof ReviewedViewHolder) {
            ReviewedViewHolder vh = (ReviewedViewHolder) holder;
            vh.tvOperator.setText(feedback.getOperator() != null ? feedback.getOperator() : "");
            vh.tvVehicleType.setText(feedback.getVehicleType() != null ? feedback.getVehicleType() : "");
            vh.tvPrice.setText(feedback.getPrice() != null ? feedback.getPrice() : "");
            vh.tvDepartureTime.setText(formatTime(feedback.getDepartureTime()));
            vh.tvOrigin.setText(feedback.getOrigin() != null ? feedback.getOrigin() : "");
            vh.tvArrivalTime.setText(formatTime(feedback.getArrivalTime()));
            vh.tvDestination.setText(feedback.getDestination() != null ? feedback.getDestination() : "");
            vh.tvDuration.setText(feedback.getDuration() != null ? feedback.getDuration() : "");
            vh.tvDate.setText(feedback.getDate() != null ? feedback.getDate() : "");
            vh.ratingBar.setRating(feedback.getRating());
            vh.tvComment.setText(feedback.getComment() != null ? feedback.getComment() : "");
            String feedbackDateFormatted = formatFeedbackDate(feedback.getFeedbackDate());
            vh.tvFeedbackDate.setText("Đã nhận xét lúc: " + feedbackDateFormatted);
        }
    }

    private String formatTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return "";
        }
        // Extract HH:mm from ISO format datetime (e.g., "2025-01-04T07:30:00" -> "07:30")
        try {
            if (dateTimeString.contains("T")) {
                return dateTimeString.split("T")[1].substring(0, 5);
            } else if (dateTimeString.contains(" ")) {
                return dateTimeString.split(" ")[1].substring(0, 5);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateTimeString;
    }

    private String formatFeedbackDate(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return "";
        }

        try {
            // Parse ISO 8601 datetime string (with or without timezone)
            LocalDateTime dateTime;

            if (dateTimeString.contains("Z")) {
                // UTC timezone (Z suffix)
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString);
                dateTime = offsetDateTime.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
            } else if (dateTimeString.contains("+") || (dateTimeString.lastIndexOf("-") > 10)) {
                // With timezone offset
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString);
                dateTime = offsetDateTime.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
            } else {
                // No timezone info, assume it's already in UTC
                dateTime = LocalDateTime.parse(dateTimeString.replace(" ", "T"));
                dateTime = dateTime.atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .toLocalDateTime();
            }

            // Format: "05/01/2025 14:30"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            e.printStackTrace();
            return dateTimeString;
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    public static class PendingViewHolder extends RecyclerView.ViewHolder {
        TextView tvOperator, tvVehicleType, tvPrice, tvDepartureTime, tvOrigin, tvArrivalTime, tvDestination, tvDuration, tvDate;
        Button btnFeedback;

        public PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnFeedback = itemView.findViewById(R.id.btnFeedback);
        }
    }

    public static class ReviewedViewHolder extends RecyclerView.ViewHolder {
        TextView tvOperator, tvVehicleType, tvPrice, tvDepartureTime, tvOrigin, tvArrivalTime, tvDestination, tvDuration, tvDate;
        TextView tvComment, tvFeedbackDate;
        RatingBar ratingBar;

        public ReviewedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFeedbackDate = itemView.findViewById(R.id.tvFeedbackDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
